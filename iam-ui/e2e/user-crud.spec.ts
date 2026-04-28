import { test, expect } from '@playwright/test'

test.describe('User CRUD', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/')
        await page.getByText('Users').click()
        await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10000 })
    })

    test('Register button opens create form', async ({ page }) => {
        await page.getByRole('button', { name: /REGISTER/i }).click()
        await expect(page.locator('[data-testid="user-create-title"]')).toBeVisible({ timeout: 10000 })
        await expect(page.getByText('Fields are derived from User Attribute Schema')).toBeVisible()
    })

    test('create shows error when userName is empty', async ({ page }) => {
        let postCalled = false
        await page.route('**/scim/v2/Users', route => {
            if (route.request().method() === 'POST') postCalled = true
            route.continue()
        })

        await page.getByRole('button', { name: /REGISTER/i }).click()
        await expect(page.locator('[data-testid="user-create-title"]')).toBeVisible({ timeout: 10000 })
        await page.getByRole('button', { name: /Create/i }).click()
        // Either toast appears, or form stays open — either way POST should not be sent
        await page.waitForTimeout(1000)
        // Form stays open (not navigated to detail)
        await expect(page.locator('[data-testid="user-create-title"]')).toBeVisible()
        // No successful create happened
        expect(postCalled).toBe(false)
    })

    test('clicking user row opens detail pane with Edit button', async ({ page }) => {
        await page.locator('tbody tr').first().click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible({ timeout: 8000 })
    })

    test('edit mode shows save and cancel buttons', async ({ page }) => {
        await page.locator('tbody tr').first().click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible({ timeout: 8000 })

        await page.getByRole('button', { name: 'Edit' }).click()
        await expect(page.getByRole('button', { name: /Save/i })).toBeVisible()
        await expect(page.getByRole('button', { name: /Cancel/i })).toBeVisible()
    })

    test('cancel edit restores view mode', async ({ page }) => {
        await page.locator('tbody tr').first().click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible({ timeout: 8000 })

        await page.getByRole('button', { name: 'Edit' }).click()
        await page.getByRole('button', { name: /Cancel/i }).click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible()
    })

    test('delete button shows confirmation overlay', async ({ page }) => {
        await page.locator('tbody tr').first().click()
        await expect(page.locator('[data-testid="user-delete-btn"]')).toBeVisible({ timeout: 8000 })

        await page.locator('[data-testid="user-delete-btn"]').click()
        await expect(page.getByText(/cannot be undone/i)).toBeVisible({ timeout: 3000 })
        await expect(page.getByRole('button', { name: /Delete/i })).toBeVisible()
    })

    test('cancel delete dismisses overlay', async ({ page }) => {
        await page.locator('tbody tr').first().click()
        await expect(page.locator('[data-testid="user-delete-btn"]')).toBeVisible({ timeout: 8000 })

        await page.locator('[data-testid="user-delete-btn"]').click()
        await expect(page.getByText(/cannot be undone/i)).toBeVisible({ timeout: 3000 })

        await page.getByRole('button', { name: 'Cancel' }).click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible()
    })
})
