import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { Employee } from '../employee.model';
import { ConfirmDeleteDialogComponent } from './confirm-delete-dialog.component';

const ASHA: Employee = {
  id: 1,
  fullName: 'Asha Patel',
  email: 'asha.patel@acme.com',
  jobTitle: 'Software Engineer',
  department: 'Engineering',
  country: 'India',
  salary: 1800000,
  currency: 'INR',
  joiningDate: '2022-04-01',
};

describe('ConfirmDeleteDialogComponent', () => {
  let fixture: ComponentFixture<ConfirmDeleteDialogComponent>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ConfirmDeleteDialogComponent, boolean>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ConfirmDeleteDialogComponent, boolean>>('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [ConfirmDeleteDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: ASHA },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDeleteDialogComponent);
    fixture.detectChanges();
  });

  it('names the employee that will be deleted', () => {
    expect(fixture.nativeElement.textContent).toContain('Asha Patel');
    expect(fixture.nativeElement.textContent).toContain('asha.patel@acme.com');
  });

  it('confirms the deletion', () => {
    fixture.nativeElement.querySelector('button[data-action="delete"]').click();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('cancels the deletion', () => {
    fixture.nativeElement.querySelector('button[data-action="cancel"]').click();

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});
