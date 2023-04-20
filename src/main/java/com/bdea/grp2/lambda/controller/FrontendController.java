package com.bdea.grp2.lambda.controller;

import com.bdea.grp2.lambda.service.FileHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Controller
@Tag(name = "Frontend API Controller", description = "Offers operations for the frontend")
public class FrontendController {
    private final FileHandler fileHandler;

    @Autowired
    public FrontendController(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

    @RequestMapping(
            path = "/upload",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "upload a file and create a tag cloud")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            boolean success = fileHandler.createTagCloud(file);
            if (success) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: ", e);
        }

    }
}
