import { Component, HostListener, input, output } from '@angular/core';

@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    <div class="modal-backdrop" aria-hidden="true"></div>
    <div class="modal-dialog" (click)="onDialogClick($event)" role="dialog" aria-modal="true" [attr.aria-label]="title()">
      <div class="modal-content card" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2 class="modal-title">{{ title() }}</h2>
          <button type="button" class="close-btn" (click)="closeModal.emit()" aria-label="Close dialog">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <ng-content></ng-content>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background-color: rgba(15, 23, 42, 0.5);
      backdrop-filter: blur(2px);
      z-index: 50;
    }
    .modal-dialog {
      position: fixed;
      inset: 0;
      z-index: 55;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: var(--space-md);
      overflow-y: auto;
    }
    .modal-content {
      width: 100%;
      max-width: 540px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: var(--shadow-lg);
      padding: var(--space-xl);
    }
    .modal-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--space-lg);
      padding-bottom: var(--space-sm);
      border-bottom: 1px solid var(--color-border);
    }
    .modal-title {
      font-size: var(--font-size-lg);
      font-weight: 700;
      color: var(--color-text-main);
    }
    .close-btn {
      background: none;
      border: none;
      color: var(--color-text-secondary);
      cursor: pointer;
      padding: 4px;
      border-radius: var(--radius-sm);
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background-color: var(--color-bg);
        color: var(--color-text-main);
      }
    }
    .modal-body {
      display: flex;
      flex-direction: column;
    }
  `]
})
export class Modal {
  readonly title = input.required<string>();
  readonly closeModal = output<void>();

  @HostListener('document:keydown.escape')
  onEscape() {
    this.closeModal.emit();
  }

  onDialogClick(event: MouseEvent) {
    if (event.target === event.currentTarget) {
      this.closeModal.emit();
    }
  }
}
