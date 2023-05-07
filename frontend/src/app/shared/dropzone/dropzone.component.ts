import {
  Component,
  ElementRef,
  ViewChild
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, catchError, combineLatest } from 'rxjs';
import { FilesService } from 'src/app/api/services/files.service';

@Component({
  selector: 'app-dropzone',
  templateUrl: './dropzone.component.html',
  styleUrls: ['./dropzone.component.scss'],
})
export class DropzoneComponent {
  @ViewChild('fileDropzone', { static: false }) fileDropEl!: ElementRef;

  public array = Array;
  uploadForm: FormGroup;
  private _loadingUpload$$ = new BehaviorSubject<boolean>(false);
  public loadingUpload$ = this._loadingUpload$$.asObservable();
  private _loadingBatch$$ = new BehaviorSubject<boolean>(false);
  public loadingBatch$ = this._loadingBatch$$.asObservable();

  constructor(
    private formBuilder: FormBuilder,
    private readonly _filesService: FilesService,
    private _snackBar: MatSnackBar
  ) {
    this.uploadForm = this.formBuilder.group({
      file: [''],
    });
  }

  public upload() {
    const files = this.fileDropEl.nativeElement.files;
    this._loadingUpload$$.next(true);
    if (files.length === 0) return;
    const uploads: Observable<string>[] = [];
    [...files].forEach((file: File) => {
      uploads.push(this._filesService.uploadFile(file));
    });
    combineLatest(uploads)
      .pipe(
        catchError((err) => {
          this.showError(err.body.message.split(';')[0]);
          return '';
        })
      )
      .subscribe((x) => {
        console.log(x);
        this._loadingUpload$$.next(false);
        this._filesService.updateFileList();
      });
    this.fileDropEl.nativeElement.value = '';
  }

  public removeFile(index: number, event: MouseEvent) {
    event.preventDefault();
    const dt = new DataTransfer();
    const files = this.fileDropEl.nativeElement.files;
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      if (index !== i) dt.items.add(file);
    }
    this.fileDropEl.nativeElement.files = dt.files;
  }

  public runBatchJob() {
    this._loadingBatch$$.next(true);
    this._filesService
      .runBatch()
      .pipe(
        catchError((err) => {
          this.showError(err.body.message);
          return '';
        })
      )
      .subscribe(() => {
        this._loadingBatch$$.next(false);
        this._filesService.updateFileList();
      });
  }

  private showError(error: string) {
    this._snackBar.open(error, 'Close', {
      horizontalPosition: 'right',
      verticalPosition: 'top',
      duration: 5000,
    });
  }
}
