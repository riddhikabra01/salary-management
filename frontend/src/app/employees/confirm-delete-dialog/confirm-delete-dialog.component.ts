import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';

import { Employee } from '../employee.model';

@Component({
  selector: 'app-confirm-delete-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>Delete employee?</h2>
    <mat-dialog-content>
      This will permanently remove <strong>{{ employee.fullName }}</strong> ({{ employee.email }}).
      This cannot be undone.
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button type="button" data-action="cancel" (click)="dialogRef.close(false)">Cancel</button>
      <button mat-flat-button color="warn" type="button" data-action="delete" (click)="dialogRef.close(true)">
        Delete
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmDeleteDialogComponent {
  readonly dialogRef = inject<MatDialogRef<ConfirmDeleteDialogComponent, boolean>>(MatDialogRef);
  readonly employee = inject<Employee>(MAT_DIALOG_DATA);
}
