import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { FilterOptions } from '../../insights/insights.model';
import { Employee, EmployeeRequest } from '../employee.model';
import { EmployeeService } from '../employee.service';

export interface EmployeeFormData {
  employee: Employee | null;
  options: FilterOptions;
}

const CURRENCY_PATTERN = /^[A-Za-z]{3}$/;

function notBlank(control: AbstractControl): ValidationErrors | null {
  const value = control.value;
  return typeof value === 'string' && value.trim() !== '' ? null : { required: true };
}

@Component({
  selector: 'app-employee-form-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './employee-form-dialog.component.html',
  styleUrl: './employee-form-dialog.component.scss',
})
export class EmployeeFormDialogComponent {
  private readonly dialogRef = inject<MatDialogRef<EmployeeFormDialogComponent, Employee>>(MatDialogRef);
  private readonly employeeService = inject(EmployeeService);

  readonly data = inject<EmployeeFormData>(MAT_DIALOG_DATA);
  readonly isEdit = this.data.employee !== null;

  readonly form = new FormGroup({
    fullName: new FormControl(this.data.employee?.fullName ?? '', { nonNullable: true, validators: [notBlank] }),
    email: new FormControl(this.data.employee?.email ?? '', {
      nonNullable: true,
      validators: [notBlank, Validators.email],
    }),
    jobTitle: new FormControl(this.data.employee?.jobTitle ?? '', { nonNullable: true, validators: [notBlank] }),
    department: new FormControl(this.data.employee?.department ?? '', { nonNullable: true, validators: [notBlank] }),
    country: new FormControl(this.data.employee?.country ?? '', { nonNullable: true, validators: [notBlank] }),
    salary: new FormControl<number | null>(this.data.employee?.salary ?? null, {
      validators: [Validators.required, Validators.min(0.01)],
    }),
    currency: new FormControl(this.data.employee?.currency ?? '', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(CURRENCY_PATTERN)],
    }),
    joiningDate: new FormControl(this.data.employee?.joiningDate ?? '', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    const request = this.toRequest();
    const call = this.data.employee
      ? this.employeeService.update(this.data.employee.id, request)
      : this.employeeService.create(request);

    call.subscribe({
      next: (saved) => this.dialogRef.close(saved),
      error: (error: HttpErrorResponse) => this.showError(error),
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  errorFor(control: AbstractControl): string {
    const errors = control.errors;
    if (!errors) {
      return '';
    }
    if (errors['server']) {
      return errors['server'];
    }
    if (errors['taken']) {
      return 'This email is already in use';
    }
    if (errors['required']) {
      return 'This field is required';
    }
    if (errors['email']) {
      return 'Enter a valid email address';
    }
    if (errors['min']) {
      return 'Must be greater than zero';
    }
    if (errors['pattern']) {
      return 'Use a 3-letter code such as USD';
    }
    return 'Invalid value';
  }

  private toRequest(): EmployeeRequest {
    const value = this.form.getRawValue();
    return {
      fullName: value.fullName.trim(),
      email: value.email.trim(),
      jobTitle: value.jobTitle.trim(),
      department: value.department.trim(),
      country: value.country.trim(),
      salary: Number(value.salary),
      currency: value.currency.trim().toUpperCase(),
      joiningDate: value.joiningDate,
    };
  }

  private showError(error: HttpErrorResponse): void {
    this.saving.set(false);

    if (error.status === 409) {
      this.markControlError('email', { taken: true });
      return;
    }

    const fieldErrors: Record<string, string> | undefined = error.error?.errors;
    if (error.status === 400 && fieldErrors && Object.keys(fieldErrors).length > 0) {
      for (const [field, message] of Object.entries(fieldErrors)) {
        this.markControlError(field, { server: message });
      }
      return;
    }

    if (error.status === 404) {
      this.errorMessage.set('This employee no longer exists.');
      return;
    }

    this.errorMessage.set('Could not save the employee. Please try again.');
  }

  private markControlError(field: string, errors: ValidationErrors): void {
    const control = this.form.get(field);
    if (control) {
      control.setErrors(errors);
      control.markAsTouched();
    }
  }
}
