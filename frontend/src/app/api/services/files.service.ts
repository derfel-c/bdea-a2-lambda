import { Injectable } from '@angular/core';
import { FrontendApiControllerService } from 'src/app/generated';

@Injectable({
  providedIn: 'root'
})
export class FilesService {
  constructor(private readonly _frontendApiControllerService: FrontendApiControllerService) {}

  public uploadFile(file: File) {
    return this._frontendApiControllerService.uploadFile({file: file});
  }

  public listFiles() {
    return this._frontendApiControllerService.listFiles();
  }

  public getTagCloud(fileName: string) {
    return this._frontendApiControllerService.getTagCloud(fileName);
  }
}
