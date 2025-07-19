package com.jobwatch.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ResumeParserService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeParserService.class);

    /**
     * Reads a .docx resume file and returns its plain text content.
     * Logs and returns null for unsupported or missing files.
     * 
     * @param filePath Path to the resume file
     * @return Resume content as text, or null if not valid
     */
    public String read(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            logger.warn(" Resume file does not exist: {}", filePath);
            return null;
        }

        if (!filePath.toLowerCase().endsWith(".docx")) {
            logger.warn(" Unsupported resume format (only .docx is supported): {}", filePath);
            return null;
        }

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            StringBuilder content = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();

            for (XWPFParagraph paragraph : paragraphs) {
                content.append(paragraph.getText()).append("\n");
            }

            logger.info(" Successfully parsed resume: {}", filePath);
            return content.toString();

        } catch (IOException e) {
            logger.error("Error reading resume file '{}': {}", filePath, e.getMessage());
            return null;
        }
    }
}
