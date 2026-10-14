import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('shows the application name in the toolbar', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const toolbar: HTMLElement = fixture.nativeElement.querySelector('mat-toolbar');
    expect(toolbar.textContent).toContain('Salary Management');
  });

  it('links to the employees page', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const link = fixture.nativeElement.querySelector('a[href="/employees"]');
    expect(link).not.toBeNull();
  });
});
