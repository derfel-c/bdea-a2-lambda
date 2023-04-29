import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropzoneComponent } from './dropzone/dropzone.component';
import { MatButtonModule } from '@angular/material/button';

@NgModule({
  exports: [DropzoneComponent],
  declarations: [DropzoneComponent],
  imports: [CommonModule, MatButtonModule],
})
export class SharedModule {}
