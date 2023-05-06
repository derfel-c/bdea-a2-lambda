/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import type { Observable } from 'rxjs';

import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

@Injectable({
  providedIn: 'root',
})
export class FrontendApiControllerService {

    constructor(public readonly http: HttpClient) {}

    /**
     * upload a file and create a tag cloud
     * @param formData
     * @returns string OK
     * @throws ApiError
     */
    public uploadFile(
formData?: {
file: Blob;
},
): Observable<string> {
  console.log('upload', formData)
        return __request(OpenAPI, this.http, {
            method: 'POST',
            url: '/upload',
            formData: formData,
            mediaType: 'multipart/form-data',
        });
    }

    /**
     * List all files from the files folder
     * @returns string OK
     * @throws ApiError
     */
    public listFiles(): Observable<Array<string>> {
        return __request(OpenAPI, this.http, {
            method: 'GET',
            url: '/listFiles',
        });
    }

    public listFilesTagCloud(): Observable<Array<string>> {
        return __request(OpenAPI, this.http, {
            method: 'GET',
            url: '/listFiles/tagCloud',
        });
    }

    public listFilesTxt(): Observable<Array<string>> {
        return __request(OpenAPI, this.http, {
            method: 'GET',
            url: '/listFiles/txt',
        });
    }

    /**
     * Get a tag cloud image
     * @param fileName
     * @returns string OK
     * @throws ApiError
     */
    public getTagCloud(
fileName: string,
): Observable<Blob> {
        return __request(OpenAPI, this.http, {
            method: 'GET',
            url: '/getTagCloud/{fileName}',
            path: {
                'fileName': fileName,
            },
            headers: {
              accept: 'image/png',
              responseType: 'blob'
            },
        });
    }

}
