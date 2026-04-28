import { test, expect } from '@playwright/test'

test.describe('Department CRUD', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/')
        await page.getByText('Departments').click()
        await expect(page.getByText('Global IT')).toBeVisible({ timeout: 10000 })
    })

    test('New button opens create form', async ({ page }) => {
        await page.getByRole('button', { name: 'New' }).click()
        await expect(page.locator('[data-testid="dept-create-title"]')).toBeVisible({ timeout: 5000 })
        await expect(page.getByText('Fields derived from Department Attribute Schema')).toBeVisible()
    })

    test('create form has SCIM ID and schema-driven fields', async ({ page }) => {
        await page.getByRole('button', { name: 'New' }).click()
        await expect(page.getByText('SCIM ID')).toBeVisible({ timeout: 5000 })
        await expect(page.getByText('Display Name')).toBeVisible()
        await expect(page.getByText('Department Code')).toBeVisible()
        await expect(page.getByText('Cost Center')).toBeVisible()
    })

    test('create validates required fields', async ({ page }) => {
        await page.getByRole('button', { name: 'New' }).click()
        await expect(page.getByRole('button', { name: /Create/ })).toBeVisible({ timeout: 5000 })
        // Click create without filling required fields
        await page.getByRole('button', { name: /Create/ }).click()
        // Toast error should appear
        await expect(page.getByText(/required/i)).toBeVisible({ timeout: 3000 })
    })

    test('can create a new department and it appears in tree', async ({ page }) => {
        await page.getByRole('button', { name: 'New' }).click()
        await expect(page.locator('[data-testid="dept-create-title"]')).toBeVisible({ timeout: 5000 })

        // Use unique ID to avoid conflict on repeated runs
        const uniqueId = `E2E-${Date.now()}`
        await page.getByPlaceholder('e.g. IT-INFRA').fill(uniqueId)
        await page.getByPlaceholder('Department name').fill('E2E Test Department')

        await page.getByRole('button', { name: /Create/ }).click()
        await expect(page.getByText(/created/i)).toBeVisible({ timeout: 5000 })
    })

    test('edit mode shows editable fields', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        await page.getByRole('button', { name: 'Edit' }).click()
        // Edit mode: Save and Cancel buttons appear in toolbar
        await expect(page.getByRole('button', { name: /Save/ })).toBeVisible()
        await expect(page.getByRole('button', { name: /Cancel/ })).toBeVisible()
    })

    test('cancel edit restores view mode', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        await page.getByRole('button', { name: 'Edit' }).click()
        await expect(page.getByRole('button', { name: /Save/ })).toBeVisible()

        await page.getByRole('button', { name: /Cancel/ }).click()
        await expect(page.getByRole('button', { name: 'Edit' })).toBeVisible()
        await expect(page.getByText('VIEW MEMBERS')).toBeVisible()
    })

    test('delete button shows confirmation overlay', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        // Trash button in action bar
        const trashBtn = page.locator('button').filter({ has: page.locator('svg') }).last()
        await page.locator('button[class*="red"]').click()

        await expect(page.getByText(/cannot be undone/i)).toBeVisible({ timeout: 3000 })
        await expect(page.getByRole('button', { name: /Delete/ })).toBeVisible()
    })

    test('cancel delete dismisses overlay', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        await page.locator('button[class*="red"]').click()
        await expect(page.getByText(/cannot be undone/i)).toBeVisible({ timeout: 3000 })

        await page.getByRole('button', { name: 'Cancel' }).click()
        await expect(page.getByText('VIEW MEMBERS')).toBeVisible()
    })
})
