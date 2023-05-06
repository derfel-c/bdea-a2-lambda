import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  BehaviorSubject,
  Observable,
  catchError,
  combineLatest
} from 'rxjs';
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
  private _loading$$ = new BehaviorSubject<boolean>(false);
  public loading$ = this._loading$$.asObservable();

  constructor(
    private formBuilder: FormBuilder,
    private readonly _filesService: FilesService,
    public cd: ChangeDetectorRef
  ) {
    this.uploadForm = this.formBuilder.group({
      file: [''],
    });
  }

  public upload() {
    const files = this.fileDropEl.nativeElement.files;
    this._loading$$.next(true);
    if (files.length === 0) return;
    const uploads: Observable<string>[] = [];
    [...files].forEach((file: File) => {
      uploads.push(this._filesService.uploadFile(file));
    });
    combineLatest(uploads)
      .pipe(
        catchError((err) => {
          console.error(err);
          this._loading$$.next(false);
          return err;
        }),
      )
      .subscribe(() => {
        this._loading$$.next(false);
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
    console.log('runBatchJob');
  }
}
