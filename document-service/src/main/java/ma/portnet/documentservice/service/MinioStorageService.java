package ma.portnet.documentservice.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import ma.portnet.documentservice.exception.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Stocke un fichier dans MinIO
     * Chemin : demandId/documentType/uuid_filename
     */
    public String store(MultipartFile file, String demandId,
                        String documentType, String fileName) {
        String objectPath = demandId + "/" + documentType + "/" + fileName;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Fichier stocké dans MinIO : {}", objectPath);
            return objectPath;
        } catch (Exception e) {
            log.error("Erreur stockage MinIO : {}", e.getMessage());
            throw new StorageException("Impossible de stocker le fichier : " + e.getMessage());
        }
    }

    /**
     * Télécharge un fichier depuis MinIO
     */
    public InputStream download(String storagePath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .build()
            );
        } catch (Exception e) {
            log.error("Erreur téléchargement MinIO : {}", e.getMessage());
            throw new StorageException("Fichier introuvable : " + storagePath);
        }
    }

    /**
     * Génère une URL présignée (lien temporaire de 1 heure)
     */
    public String generatePresignedUrl(String storagePath) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Impossible de générer l'URL : " + e.getMessage());
        }
    }

    /**
     * Supprime un fichier de MinIO
     */
    public void delete(String storagePath) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(storagePath)
                            .build()
            );
            log.info("Fichier supprimé de MinIO : {}", storagePath);
        } catch (Exception e) {
            log.error("Erreur suppression MinIO : {}", e.getMessage());
            throw new StorageException("Impossible de supprimer le fichier : " + e.getMessage());
        }
    }
}