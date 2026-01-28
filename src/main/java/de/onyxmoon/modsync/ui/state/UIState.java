package de.onyxmoon.modsync.ui.state;

import de.onyxmoon.modsync.api.model.ManagedMod;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Manages UI state per player.
 * Tracks current selection, loading state, and status messages.
 */
public class UIState {

    /**
     * Status message types for UI feedback.
     */
    public enum StatusType {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    /**
     * Page types for navigation tracking.
     */
    public enum PageType {
        MAIN,
        ADD_MOD,
        MOD_DETAIL,
        SCAN,
        CONFIG
    }

    private PageType currentPage = PageType.MAIN;
    private ManagedMod selectedMod;
    private boolean loading;
    private String statusMessage;
    private StatusType statusType;
    private CompletableFuture<?> pendingOperation;

    /**
     * Gets the current page type.
     */
    public PageType getCurrentPage() {
        return currentPage;
    }

    /**
     * Sets the current page type.
     */
    public void setCurrentPage(PageType currentPage) {
        this.currentPage = currentPage;
    }

    /**
     * Gets the currently selected mod (for detail view).
     */
    @Nullable
    public ManagedMod getSelectedMod() {
        return selectedMod;
    }

    /**
     * Sets the currently selected mod.
     */
    public void setSelectedMod(@Nullable ManagedMod selectedMod) {
        this.selectedMod = selectedMod;
    }

    /**
     * Checks if a loading operation is in progress.
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Starts a loading state.
     */
    public void startLoading() {
        this.loading = true;
        this.statusMessage = null;
        this.statusType = null;
    }

    /**
     * Stops the loading state.
     */
    public void stopLoading() {
        this.loading = false;
    }

    /**
     * Gets the current status message.
     */
    @Nullable
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Gets the current status type.
     */
    @Nullable
    public StatusType getStatusType() {
        return statusType;
    }

    /**
     * Sets a status message with type.
     */
    public void setStatus(String message, StatusType type) {
        this.statusMessage = message;
        this.statusType = type;
    }

    /**
     * Clears the current status message.
     */
    public void clearStatus() {
        this.statusMessage = null;
        this.statusType = null;
    }

    /**
     * Gets the pending async operation.
     */
    @Nullable
    public CompletableFuture<?> getPendingOperation() {
        return pendingOperation;
    }

    /**
     * Sets a pending async operation.
     * Cancels any existing pending operation.
     */
    public void setPendingOperation(@Nullable CompletableFuture<?> pendingOperation) {
        // Cancel existing operation if any
        if (this.pendingOperation != null && !this.pendingOperation.isDone()) {
            this.pendingOperation.cancel(false);
        }
        this.pendingOperation = pendingOperation;
    }

    /**
     * Cancels the current pending operation if any.
     */
    public void cancelPendingOperation() {
        if (pendingOperation != null && !pendingOperation.isDone()) {
            pendingOperation.cancel(false);
        }
        pendingOperation = null;
    }

    /**
     * Resets the state to defaults.
     */
    public void reset() {
        this.currentPage = PageType.MAIN;
        this.selectedMod = null;
        this.loading = false;
        this.statusMessage = null;
        this.statusType = null;
        cancelPendingOperation();
    }
}
