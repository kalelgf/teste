package com.programacao_web.rpg_market.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.Base64;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private final Path fileStorageLocation;    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            log.info("Criando diretório em: {}", this.fileStorageLocation.toString());
            Files.createDirectories(this.fileStorageLocation);
            log.info("Diretório de upload criado com sucesso");
        } catch (Exception ex) {
            log.error("Não foi possível criar diretório de upload: {}", ex.getMessage());
            throw new RuntimeException("Não foi possível criar o diretório de upload: " + this.fileStorageLocation, ex);
        }
    }

    public String storeFile(MultipartFile file) throws IOException {        if (file == null || file.isEmpty()) {
            log.warn("Tentativa de salvar arquivo vazio ou nulo");
            return null;
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String filename = UUID.randomUUID().toString() + fileExtension;
        Path targetLocation = this.fileStorageLocation.resolve(filename);
        
        try {
            // Ensure parent directories exist
            Files.createDirectories(targetLocation.getParent());
            
            // Copy file to the target location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully at: {}", targetLocation);
            
            return filename;
        } catch (IOException ex) {
            log.error("Could not store file: {}", ex.getMessage(), ex);
            throw new IOException("Não foi possível armazenar o arquivo. Por favor, tente novamente!", ex);
        }
    }
    
    /**
     * Returns the Path for a specific file
     */
    public Path getFilePath(String fileName) {
        return this.fileStorageLocation.resolve(fileName);
    }
    
    /**
     * Delete a file from storage
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Could not delete file: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Armazena uma imagem Base64 e retorna o nome do arquivo gerado
     */
    public String storeBase64Image(String base64Image, String prefix) throws IOException {
        // Remover prefixo "data:image/jpeg;base64," ou similar
        String[] parts = base64Image.split(",");
        String imageData = parts.length > 1 ? parts[1] : parts[0];
        
        // Decodificar a string Base64
        byte[] imageBytes = Base64.getDecoder().decode(imageData);
        
        // Gerar nome de arquivo único
        String filename = prefix + "-" + UUID.randomUUID().toString() + ".jpg";
        
        // Caminho completo para o arquivo
        Path targetLocation = this.fileStorageLocation.resolve(filename);
        
        // Salvar o arquivo
        Files.write(targetLocation, imageBytes);
        
        log.info("Base64 image stored successfully at: {}", targetLocation);
        return filename;
    }
    
    /**
     * Armazena e redimensiona uma imagem para as dimensões específicas
     */
    public String storeAndResizeImage(MultipartFile file, int width, int height) throws IOException {
        if (file == null || file.isEmpty()) {
            log.warn("Attempted to store empty or null file");
            return null;
        }
        
        // Gerar nome de arquivo único
        String filename = UUID.randomUUID().toString() + getFileExtension(file.getOriginalFilename());
        
        // Ler a imagem original
        BufferedImage originalImage = ImageIO.read(file.getInputStream());
        
        // Redimensionar para o tamanho desejado (200x200)
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        
        // Salvar a imagem redimensionada
        Path targetLocation = this.fileStorageLocation.resolve(filename);
        File outputFile = targetLocation.toFile();
        ImageIO.write(resizedImage, "jpg", outputFile);
        
        log.info("Resized image stored successfully at: {}", targetLocation);
        return filename;
    }
    
    private String getFileExtension(String originalFilename) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "";
    }
}