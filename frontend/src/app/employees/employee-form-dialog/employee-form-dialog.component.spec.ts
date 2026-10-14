import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { FilterOptions } from '../../insights/insights.model';
import { Employee } from '../employee.model';
import { EmployeeFormDialogComponent } from './employee-form-dialog.component';

const OPTIONS: FilterOptions = {
  countries: ['Germany', 'India'],
  departments: ['Analytics', 'Engineering'],
  jobTitles: ['Data Analyst', 'Software Engineer'],
};

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

const NEW_EMPLOYEE_FORM = {
  fullName: '  Rohan Mehta ',
  email: 'rohan.mehta@acme.com',
  jobTitle: 'Data Analyst',
  department: 'Analytics',
  country: 'India',
  salary: 1200000,
  currency: 'inr',
  joiningDate: '2024-01-15',
};

describe('EmployeeFormDialogComponent', () => {
  let fixture: ComponentFixture<EmployeeFormDialogComponent>;
  let component: EmployeeFormDialogComponent;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<EmployeeFormDialogComponent, Employee>>;

  function setup(employee: Employee | null): void {
    dialogRef = jasmine.createSpyObj<MatDialogRef<EmployeeFormDialogComponent, Employee>>('MatDialogRef', ['close']);

    TestBed.configureTestingModule({
      imports: [EmployeeFormDialogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: MAT_DIALOG_DATA, useValue: { employee, options: OPTIONS } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });

    fixture = TestBed.createComponent(EmployeeFormDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('starts empty when adding an employee', () => {
    setup(null);

    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Add employee');
    expect(component.form.controls.fullName.value).toBe('');
    expect(component.form.controls.salary.value).toBeNull();
  });

  it('is filled in when editing an employee', () => {
    setup(ASHA);

    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Edit employee');
    expect(component.form.controls.fullName.value).toBe('Asha Patel');
    expect(component.form.controls.salary.value).toBe(1800000);
  });

  it('does not send an incomplete form', () => {
    setup(null);

    component.submit();

    http.expectNone('/api/employees');
    expect(component.form.controls.fullName.touched).toBeTrue();
    expect(component.form.controls.fullName.hasError('required')).toBeTrue();
  });

  it('rejects a currency that is not three letters', () => {
    setup(null);

    component.form.controls.currency.setValue('US');

    expect(component.form.controls.currency.hasError('pattern')).toBeTrue();
  });

  it('rejects a salary of zero', () => {
    setup(null);

    component.form.controls.salary.setValue(0);

    expect(component.form.controls.salary.hasError('min')).toBeTrue();
  });

  it('creates an employee with tidied values and closes the dialog', () => {
    setup(null);
    const created: Employee = { id: 9, ...NEW_EMPLOYEE_FORM, fullName: 'Rohan Mehta', currency: 'INR' };
    component.form.setValue(NEW_EMPLOYEE_FORM);

    component.submit();

    const call = http.expectOne('/api/employees');
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual({
      fullName: 'Rohan Mehta',
      email: 'rohan.mehta@acme.com',
      jobTitle: 'Data Analyst',
      department: 'Analytics',
      country: 'India',
      salary: 1200000,
      currency: 'INR',
      joiningDate: '2024-01-15',
    });
    call.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('updates an existing employee', () => {
    setup(ASHA);
    component.form.controls.salary.setValue(2400000);

    component.submit();

    const call = http.expectOne('/api/employees/1');
    expect(call.request.method).toBe('PUT');
    expect(call.request.body.salary).toBe(2400000);
    const updated = { ...ASHA, salary: 2400000 };
    call.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
  });

  it('shows that the email is already taken', () => {
    setup(null);
    component.form.setValue(NEW_EMPLOYEE_FORM);

    component.submit();
    http
      .expectOne('/api/employees')
      .flush({ message: 'An employee with email rohan.mehta@acme.com already exists' }, { status: 409, statusText: 'Conflict' });

    expect(component.form.controls.email.hasError('taken')).toBeTrue();
    expect(component.errorFor(component.form.controls.email)).toBe('This email is already in use');
    expect(component.saving()).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('shows field errors reported by the server', () => {
    setup(null);
    component.form.setValue(NEW_EMPLOYEE_FORM);

    component.submit();
    http.expectOne('/api/employees').flush(
      { message: 'Validation failed', errors: { salary: 'Salary must be greater than zero' } },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(component.errorFor(component.form.controls.salary)).toBe('Salary must be greater than zero');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('shows a general message when saving fails unexpectedly', () => {
    setup(null);
    component.form.setValue(NEW_EMPLOYEE_FORM);

    component.submit();
    http.expectOne('/api/employees').flush('Oops', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const alert: HTMLElement = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert.textContent).toContain('Could not save the employee');
    expect(component.saving()).toBeFalse();
  });

  it('closes without saving when cancelled', () => {
    setup(ASHA);

    fixture.nativeElement.querySelector('button[data-action="cancel"]').click();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone('/api/employees/1');
  });
});
