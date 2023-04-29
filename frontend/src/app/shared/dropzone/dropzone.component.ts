import { ChangeDetectorRef, Component, ElementRef, ViewChild } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { FilesService } from 'src/app/api/services/files.service';

@Component({
  selector: 'app-dropzone',
  templateUrl: './dropzone.component.html',
  styleUrls: ['./dropzone.component.scss'],
})
export class DropzoneComponent {
  @ViewChild('fileDropzone', { static: false }) fileDropEl!: ElementRef;

  constructor(private readonly _filesService: FilesService, public cd: ChangeDetectorRef) {}

  public upload() {
    const files = this.fileDropEl.nativeElement.files;
    console.log(this.fileDropEl.nativeElement.files, [...files]);
    if (files.length === 0) return;
    const uploads: Observable<string>[]    = [];
    [...files].forEach((file: File) => {
      uploads.push(this._filesService.uploadFile(file));
    });
    combineLatest(uploads).subscribe();
    this.fileDropEl.nativeElement.value = '';
  }
}
