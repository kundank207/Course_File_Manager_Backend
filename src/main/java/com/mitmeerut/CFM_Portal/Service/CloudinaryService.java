package com.mitmeerut.CFM_Portal.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload image to Cloudinary
     * 
     * @param file   the image file to upload
     * @param folder the folder name in Cloudinary
     * @return the secure URL of the uploaded image
     */
    public String uploadImage(MultipartFile file, String folder) throws IOException {
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"));
        return (String) result.get("secure_url");
    }

    /**
     * Upload image from base64 string
     * 
     * @param base64Image the base64 encoded image data
     * @param folder      the folder name in Cloudinary
     * @return the secure URL of the uploaded image
     */
    public String uploadBase64Image(String base64Image, String folder) throws IOException {
        // If it contains data URI prefix, keep it; otherwise add it
        String dataUri = base64Image;
        if (!base64Image.startsWith("data:")) {
            dataUri = "data:image/png;base64," + base64Image;
        }

        Map<?, ?> result = cloudinary.uploader().upload(dataUri,
                ObjectUtils.asMap(
                        "folder", folder,
                        "resource_type", "auto"));
        return (String) result.get("secure_url");
    }

    /**
     * Delete image from Cloudinary by public ID
     */
    public void deleteImage(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }
}
