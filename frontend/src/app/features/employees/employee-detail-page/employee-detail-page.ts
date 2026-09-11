import { Component, input } from '@angular/core';

@Component({
  selector: 'app-employee-detail-page',
  template: `
    <h2>Employee Detail</h2>
    <p>Employee id: {{ id() }}</p>
  `,
})
export class EmployeeDetailPage {
  readonly id = input<string>();
}
