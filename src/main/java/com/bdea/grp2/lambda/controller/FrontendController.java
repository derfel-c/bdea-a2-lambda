package com.bdea.grp2.lambda.controller;

import com.bdea.grp2.lambda.service.FileHandler;
import com.bdea.grp2.lambda.service.SparkService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.AnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;

@Slf4j
@RestController
@Api(value = "Frontend API Controller")
public class FrontendController {
    private final FileHandler fileHandler;
    private final SparkService sparkService;

    @Autowired
    public FrontendController(FileHandler fileHandler, SparkService sparkService) {
        this.fileHandler = fileHandler;
        this.sparkService = sparkService;
    }

    @RequestMapping(
            path = "/upload",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "upload a file and create a tag cloud")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            boolean txtSaveSuccess = fileHandler.saveTextFile(file);
            boolean createTagCloudSuccess = fileHandler.createTagCloud(file);
            if (createTagCloudSuccess && txtSaveSuccess) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } catch (IOException | AnalysisException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Upload failed: ", e);
        }

    }

    @GetMapping(value = "/listFiles")
    @Operation(summary = "List all files from the files folder")
    public Set<String> listFiles() {
        try {
            Set<String> tagClouds = this.fileHandler.listTagClouds();
            Set<String> txtFiles = this.fileHandler.listTxtFiles();
            tagClouds.addAll(txtFiles);
            return tagClouds;
        } catch (IOException e) {
            log.error("Failed to list files", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list files: " + e.getMessage());
        }
    }

    @GetMapping(value = "/listFiles/txt")
    @Operation(summary = "List all uploaded .txt files")
    public Set<String> listTxtFiles() {
        try {
            return this.fileHandler.listTxtFiles();
        } catch (IOException e) {
            log.error("Failed to list files", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list files: " + e.getMessage());
        }
    }

    @GetMapping(value = "/listFiles/tagCloud")
    @Operation(summary = "List all tag cloud files")
    public Set<String> listTagClouds() {
        try {
            return this.fileHandler.listTagClouds();
        } catch (IOException e) {
            log.error("Failed to list files", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to list files: " + e.getMessage());
        }
    }

    @GetMapping(value = "/getTagCloud/{fileName}",
            produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Get a tag cloud image")
    public byte[] getTagCloud(@PathVariable String fileName) {
        try {
            return this.fileHandler.getTagCloud(fileName);
        } catch (IOException e) {
            log.error("Failed to get tag cloud", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get tag cloud: " + e.getMessage());
        }
    }
}
