import { test, expect } from '@playwright/test'

test.describe('DeptManagement', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/')
        await page.getByText('Departments').click()
        // Wait for tree to load
        await expect(page.getByText('Global IT')).toBeVisible({ timeout: 10000 })
    })

    test('shows full department tree in one column', async ({ page }) => {
        // Root depts visible without any clicks
        await expect(page.getByText('Global IT')).toBeVisible()
        await expect(page.getByText('Internal Audit')).toBeVisible()
        await expect(page.getByText('SAP HR Division')).toBeVisible()
        await expect(page.getByText('External Vendors')).toBeVisible()
    })

    test('clicking department opens detail pane with schema-driven attributes', async ({ page }) => {
        await page.getByText('Global IT').click()

        // Detail pane toolbar appears
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        // Attribute count badge — must be > 0
        const countText = await page.locator('[data-testid="dept-attr-count"]').textContent()
        const match = countText?.match(/\((\d+)\)/)
        expect(match).not.toBeNull()
        expect(parseInt(match![1])).toBeGreaterThan(0)
    })

    test('dept detail shows all 12 schema attributes', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        const countText = await page.locator('[data-testid="dept-attr-count"]').textContent()
        const count = parseInt(countText?.match(/\((\d+)\)/)?.[1] ?? '0')
        expect(count).toBe(12)
    })

    test('dept detail shows actual attribute values from API', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        // deptCode value from sample data
        await expect(page.getByText('GITD')).toBeVisible()
        // manager name
        await expect(page.getByText('James Kang')).toBeVisible()
        // cost center
        await expect(page.getByText('CC-1000')).toBeVisible()
    })

    test('different departments show their own attribute values', async ({ page }) => {
        // Click Internal Audit
        await page.getByText('Internal Audit').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })

        await expect(page.getByText('AUDT')).toBeVisible()
        await expect(page.getByText('Daniel Lim')).toBeVisible()
        await expect(page.getByText('CC-2000')).toBeVisible()
    })

    test('VIEW MEMBERS button is present in detail pane', async ({ page }) => {
        await page.getByText('Global IT').click()
        await expect(page.locator('[data-testid="dept-attr-toolbar"]')).toBeVisible({ timeout: 8000 })
        await expect(page.getByText('VIEW MEMBERS')).toBeVisible()
    })
})
