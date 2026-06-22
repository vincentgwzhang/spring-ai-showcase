package pl.piomin.services.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VectorService {

    private final EmbeddingModel embeddingModel;

    // ===============================
    // Config (可调参数)
    // ===============================
    @Value("${rag.topK:3}")
    private int topK;

    @Value("${rag.chunkSize:300}")
    private int chunkSize;

    @Value("${rag.chunkOverlap:50}")
    private int chunkOverlap;

    // ===============================
    // In-memory store
    // ===============================
    private final List<String> chunks = new ArrayList<>();
    private final List<float[]> vectors = new ArrayList<>();

    public VectorService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @PostConstruct
    void init() {
        loadPdfAndIndex("data/refund_policy.pdf");
    }

    // ===============================
    // Step 1: Load PDF
    // ===============================
    private String readPdf(String path) {
        try (PDDocument document = Loader.loadPDF(new File(path))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PDF: " + path, e);
        }
    }

    // ===============================
    // Step 2: Chunking (with overlap ✅)
    // ===============================
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                continue;
            }
            if (currentChunk.length() + paragraph.length() > chunkSize) {
                if (!currentChunk.isEmpty()) {
                    chunks.add(currentChunk.toString().trim());
                }
                int overlapStart = Math.max(0, currentChunk.length() - chunkOverlap);
                currentChunk = new StringBuilder(
                    currentChunk.substring(overlapStart)
                );
            }
            currentChunk.append(paragraph).append("\n\n");
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString().trim());
        }
        return chunks;
    }

    // ===============================
    // Step 3: Embed + Store
    // ===============================
    private void loadPdfAndIndex(String path) {
        String text = readPdf(path);
        List<String> splitChunks = chunkText(text);

        System.out.println("Chunking completed. Total chunks: " + splitChunks.size());
        for (String chunk : splitChunks) {
            float[] embedding = embeddingModel.embed(chunk);
            chunks.add(chunk);
            vectors.add(embedding);
        }

        System.out.println("Indexing completed. Stored vectors: " + vectors.size());
    }

    // ===============================
    // Step 4: Search
    // ===============================
    public List<String> search(String query) {

        if (vectors.isEmpty()) {
            throw new IllegalStateException("Vector store is empty. Did indexing run?");
        }

        float[] queryVector = embeddingModel.embed(query);

        List<Pair> scored = new ArrayList<>();

        for (int i = 0; i < vectors.size(); i++) {
            double score = cosineSimilarity(queryVector, vectors.get(i));
            scored.add(new Pair(chunks.get(i), score));
        }

        // 排序（从高到低）
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // ===============================
        // Debug（非常重要 🔥）
        // ===============================
        System.out.println("==== Retrieval Debug ====");
        System.out.println("Query: " + query);

        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            System.out.println("Score: " + scored.get(i).score);
            System.out.println("Text: " + scored.get(i).text);
            System.out.println("----------------------");
        }

        // ===============================
        // Top-K 返回
        // ===============================
        List<String> results = new ArrayList<>();

        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            results.add(scored.get(i).text);
        }

        return results;
    }

    // ===============================
    // Cosine Similarity
    // ===============================
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot   += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0;

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ===============================
    // Internal Pair class
    // ===============================
    private static class Pair {
        final String text;
        final double score;

        Pair(String text, double score) {
            this.text = text;
            this.score = score;
        }
    }
}
