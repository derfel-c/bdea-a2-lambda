import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { FileListModule } from './file-list/file-list.module';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientModule } from '@angular/common/http';
import { SharedModule } from './shared/shared.module';
import { WordcloudModule } from './wordcloud/wordcloud.module';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    FileListModule,
    BrowserAnimationsModule,
    HttpClientModule,
    SharedModule,
    WordcloudModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
