import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropzoneComponent } from './dropzone/dropzone.component';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@NgModule({
  exports: [DropzoneComponent],
  declarations: [DropzoneComponent],
  imports: [CommonModule, MatButtonModule, MatIconModule, FormsModule, ReactiveFormsModule],
})
export class SharedModule {}
