import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FileListComponent } from './components/file-list/file-list.component';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';

@NgModule({
  exports: [FileListComponent],
  declarations: [FileListComponent],
  imports: [
    CommonModule,
    FormsModule,
    MatSelectModule,
    MatInputModule,
    MatTableModule,
  ],
})
export class FileListModule {}
