import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { Observable, combineLatest } from 'rxjs';
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

  constructor(
    private formBuilder: FormBuilder,
    private readonly _filesService: FilesService,
    public cd: ChangeDetectorRef
  ) {
    this.uploadForm = this.formBuilder.group({
      file: ['']
    });
  }

  public upload() {
    const files = this.fileDropEl.nativeElement.files;
    console.log(this.fileDropEl.nativeElement.files, [...files]);
    if (files.length === 0) return;
    const uploads: Observable<string>[] = [];
    [...files].forEach((file: File) => {
      uploads.push(this._filesService.uploadFile(file));
    });
    combineLatest(uploads).subscribe();
    this.fileDropEl.nativeElement.value = '';
  }

  public removeFile(index: number, event: MouseEvent) {
    event.preventDefault();
    const dt = new DataTransfer();
    const files = this.fileDropEl.nativeElement.files;
    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      if (index !== i)
        dt.items.add(file)
    }
    this.fileDropEl.nativeElement.files = dt.files
  }
}
