import { Component, EventEmitter, Output } from '@angular/core';
import { map } from 'rxjs';
import { FilesService } from 'src/app/api/services/files.service';

@Component({
  selector: 'app-file-list',
  templateUrl: './file-list.component.html',
  styleUrls: ['./file-list.component.scss'],
})
export class FileListComponent {

  @Output() public fileSelected = new EventEmitter<string>();

  public files$ = this._filesService.listFiles();

  public columns = ['name']

  constructor(private readonly _filesService: FilesService) {}
}
