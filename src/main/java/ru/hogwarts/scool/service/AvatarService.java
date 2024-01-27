package ru.hogwarts.scool.service;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.hogwarts.scool.model.Avatar;
import ru.hogwarts.scool.model.Student;
import ru.hogwarts.scool.repository.AvatarRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.CREATE_NEW;

@Service
@Transactional
public class AvatarService {
    @Value("${students.avatar.dir.path}")
    String avatarsDir;
    private final StudentService studentService;
    private final AvatarRepository avatarRepository;

    private final Logger logger = LoggerFactory.getLogger(AvatarService.class);

    public AvatarService(StudentService studentService, AvatarRepository avatarRepository) {
        this.studentService = studentService;
        this.avatarRepository = avatarRepository;
    }

    public void uploaderAvatar(long studentId, MultipartFile file) throws IOException {
        logger.info("Был вызван метод uploaderAvatar");
        Student student = studentService.readStudent(studentId);

        Path filePath = Path.of(avatarsDir, studentId + "." + getExtension(file.getOriginalFilename()));
        Files.createDirectories(filePath.getParent());
        Files.deleteIfExists(filePath);

        try (InputStream is = file.getInputStream();
             OutputStream os = Files.newOutputStream(filePath, CREATE_NEW);
             BufferedInputStream bis = new BufferedInputStream(is, 1024);
             BufferedOutputStream bos = new BufferedOutputStream(os, 1024);
        ) {
            bis.transferTo(bos);
            Avatar avatar = findAvatar(studentId);
            avatar.setStudent(student);
            avatar.setFilePath(filePath.toString());
            avatar.setFileSize(file.getSize());
            avatar.setMediaType(file.getContentType());
            avatar.setData(generateDataForDB(filePath));

            avatarRepository.save(avatar);

        }
    }

    public Avatar findAvatar(long studentId) {
        logger.info("Был вызван метод findAvatar");
        return avatarRepository.findByStudentId(studentId).orElseGet(Avatar::new);
    }

    public String getExtension(String fileName) {
        logger.info("Был вызван метод getExtension");
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private byte[] generateDataForDB(Path filePath) throws IOException {
        logger.info("Был вызван метод generateDataForDB");
        try (InputStream is = Files.newInputStream(filePath);
             BufferedInputStream bis = new BufferedInputStream(is, 1024);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            BufferedImage image = ImageIO.read(bis);
            int height = image.getHeight() / (image.getWidth() / 100);
            BufferedImage preview = new BufferedImage(100, height, image.getType());
            Graphics2D graphics = preview.createGraphics();
            graphics.drawImage(
                    image,
                    0,
                    0,
                    100,
                    height,
                    null
            );
            graphics.dispose();

            ImageIO.write(preview, "jpg", baos);
            return baos.toByteArray();
        }
    }

    public Page<Avatar> getAllAvatars(Integer pageNumber, Integer pageSize) {
        logger.info("Был вызван метод getAllAvatars");
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return avatarRepository.findAll(pageable);

    }

}
