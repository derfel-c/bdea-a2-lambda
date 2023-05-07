import { Injectable } from '@angular/core';
import { BehaviorSubject, switchMap } from 'rxjs';
import { FrontendApiControllerService } from 'src/app/generated';

@Injectable({
  providedIn: 'root'
})
export class FilesService {

  private _updateFileList$$ = new BehaviorSubject<void>(undefined);

  constructor(private readonly _frontendApiControllerService: FrontendApiControllerService) {}

  public uploadFile(file: File) {
    return this._frontendApiControllerService.uploadFile({file: file});
  }

  public listFiles() {
    return this._frontendApiControllerService.listFiles();
  }

  public listFilesTagCloud() {
    return this._updateFileList$$.pipe(
      switchMap(() => this._frontendApiControllerService.listFilesTagCloud())
    );
  }

  public listFilesTxt() {
    return this._frontendApiControllerService.listFilesTxt();
  }

  public getTagCloud(fileName: string) {
    return this._frontendApiControllerService.getTagCloud(fileName);
  }

  public updateFileList() {
    this._updateFileList$$.next(undefined);
  }

  public runBatch() {
    return this._frontendApiControllerService.runBatch();
  }
}
