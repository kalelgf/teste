package com.programacao_web.rpg_market.controller;

import com.programacao_web.rpg_market.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/images")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            if ("default-product.jpg".equals(fileName)) {
                try {
                    Resource resource = new UrlResource(getClass().getClassLoader().getResource("static/images/default-product.jpg"));
                    if (resource.exists()) {
                        return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(resource);
                    }
                } catch (Exception e) {
                    log.error("Erro ao carregar imagem padrão: {}", e.getMessage());
                }
            }
            
            try {
                Resource staticResource = new UrlResource(getClass().getClassLoader().getResource("static/images/" + fileName));
                if (staticResource != null && staticResource.exists()) {
                    MediaType mediaType = determineMediaType(fileName);
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + staticResource.getFilename() + "\"")
                        .contentType(mediaType)
                        .body(staticResource);
                }
            } catch (Exception e) {
                log.debug("Arquivo estático não encontrado: {}", fileName);
            }
            
            try {
                Path filePath = fileStorageService.getFilePath(fileName);
                Resource resource = new UrlResource(filePath.toUri());
                
                if (resource.exists()) {
                    MediaType mediaType = determineMediaType(fileName);
                      return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(mediaType)
                        .body(resource);
                }
            } catch (Exception e) {
                log.error("Erro ao carregar arquivo de upload: {}", e.getMessage());
            }
            
            // Fallback para imagem padrão em arquivos UUID não encontrados
            if (fileName.matches("^[a-f0-9-]{36}\\.(jpg|jpeg|png|gif|webp)$")) {
                try {
                    Resource defaultResource = new UrlResource(getClass().getClassLoader().getResource("static/images/default-product.jpg"));
                    if (defaultResource != null && defaultResource.exists()) {
                        return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"default-product.jpg\"")
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(defaultResource);
                    }
                } catch (Exception e) {
                    log.error("Erro ao carregar imagem padrão: {}", e.getMessage());
                }
            }
            
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            log.error("Erro ao servir arquivo {}: {}", fileName, e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
    
    @GetMapping("/debug/path")
    @ResponseBody
    public Map<String, Object> debugPath() {
        Map<String, Object> info = new HashMap<>();
        Path uploadDir = fileStorageService.getFilePath("");
        info.put("diretorioUpload", uploadDir.toString());
        info.put("existe", Files.exists(uploadDir));
        info.put("ehDiretorio", Files.isDirectory(uploadDir));
        info.put("ehLegivel", Files.isReadable(uploadDir));        
        try {
            List<String> files = Files.list(uploadDir)
                .map(path -> path.getFileName().toString())
                .sorted()
                .collect(Collectors.toList());
            info.put("arquivos", files);
        } catch (Exception e) {
            info.put("erroArquivos", e.getMessage());
        }
        
        return info;
    }

    @GetMapping("/debug/test")
    @ResponseBody
    public String test() {
        return "FileController funcionando!";
    }

    private MediaType determineMediaType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}