import { Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="empty-state-container">
      <div class="empty-icon">
        <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
      </div>
      <h3 class="empty-title">{{ title() }}</h3>
      <p class="empty-description">{{ description() }}</p>
      <div class="empty-actions">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .empty-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: var(--space-2xl) var(--space-lg);
      text-align: center;
      background-color: var(--color-surface);
      border: 1px dashed var(--color-border);
      border-radius: var(--radius-lg);
    }
    .empty-icon {
      color: var(--color-text-muted);
      margin-bottom: var(--space-sm);
    }
    .empty-title {
      font-size: var(--font-size-base);
      font-weight: 600;
      color: var(--color-text-main);
    }
    .empty-description {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      max-width: 400px;
      margin-top: var(--space-2xs);
      margin-bottom: var(--space-md);
    }
  `]
})
export class EmptyState {
  readonly title = input<string>('No records found');
  readonly description = input<string>('Try adjusting your search or filter parameters.');
}
