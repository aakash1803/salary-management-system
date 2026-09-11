import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  standalone: true,
  template: `
    <header class="page-header">
      <div class="header-titles">
        <h1 class="page-title">{{ title() }}</h1>
        @if (description()) {
          <p class="page-description">{{ description() }}</p>
        }
      </div>
      <div class="header-actions">
        <ng-content></ng-content>
      </div>
    </header>
  `,
  styles: [`
    .page-header {
      display: flex;
      flex-direction: column;
      gap: var(--space-sm);
      margin-bottom: var(--space-xl);
    }
    @media (min-width: 640px) {
      .page-header {
        flex-direction: row;
        align-items: center;
        justify-content: space-between;
      }
    }
    .page-title {
      font-size: var(--font-size-2xl);
      font-weight: 700;
      color: var(--color-text-main);
      letter-spacing: -0.02em;
    }
    .page-description {
      font-size: var(--font-size-sm);
      color: var(--color-text-secondary);
      margin-top: var(--space-2xs);
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: var(--space-sm);
    }
  `]
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly description = input<string>();
}
