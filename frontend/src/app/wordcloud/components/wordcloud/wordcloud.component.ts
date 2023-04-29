import { Component, Input } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { Subject } from 'rxjs';
import { map, shareReplay, switchMap, tap } from 'rxjs/operators';
import { FilesService } from 'src/app/api/services/files.service';

@Component({
  selector: 'app-wordcloud',
  templateUrl: './wordcloud.component.html',
  styleUrls: ['./wordcloud.component.scss'],
})
export class WordcloudComponent {
  private _file$$ = new Subject<string>();
  private _wordcloud$$ = this._file$$.pipe(
    switchMap((file: string) =>
      this._filesService.getTagCloud(file).pipe(shareReplay(1))
    ),
    map((blob) => URL.createObjectURL(blob))
  );
  public wordcloud$ = this._wordcloud$$.pipe(shareReplay(1));
  @Input() public set file(file: string | undefined) {
    if (file) {
      this._file$$.next(file);
    }
  }

  constructor(private readonly _filesService: FilesService) {}
}
