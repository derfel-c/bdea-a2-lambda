import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WordcloudComponent } from './components/wordcloud/wordcloud.component';



@NgModule({
  exports: [WordcloudComponent],
  declarations: [
    WordcloudComponent
  ],
  imports: [
    CommonModule
  ]
})
export class WordcloudModule { }
