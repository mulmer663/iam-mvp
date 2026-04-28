import { test, expect } from '@playwright/test'

const DEPT_URN = 'urn:iam:params:scim:schemas:2.0:Department'

test.describe('SchemaDetailPane', () => {
    test.beforeEach(async ({ page }) => {
        await page.goto('/')
        await page.getByText('Attribute Schema').click()
        await expect(page.getByText('SCHEMAS').first()).toBeVisible({ timeout: 10000 })
    })

    test('Department schema list count is not same as User', async ({ page }) => {
        // User row shows "62 attrs", Department should show a different (smaller) number
        const userAttrsText = await page.locator('text=62 attrs').count()
        expect(userAttrsText).toBe(1)  // only User should show 62, not Department too
    })

    test('Department schema detail shows small attribute count', async ({ page }) => {
        // Click Department schema row — use first() since URN appears in both Schemas & Resource Types lists
        await page.locator(`text=${DEPT_URN}`).first().click()

        // Wait for SchemaDetailPane toolbar
        await expect(page.locator('[data-testid="attr-section-toolbar"]')).toBeVisible({ timeout: 10000 })

        // Read the count badge
        const countText = await page.locator('[data-testid="attr-count"]').textContent()
        const match = countText?.match(/\((\d+)\)/)

        expect(match).not.toBeNull()
        const count = parseInt(match![1])

        // Must NOT be 62 (User attribute count — the old bug)
        expect(count).not.toBe(62)
        // Department has ~4 core attrs
        expect(count).toBeLessThan(20)
    })

    test('User schema detail shows user attributes', async ({ page }) => {
        await page.locator('text=core:2.0:User').first().click()

        await expect(page.locator('[data-testid="attr-section-toolbar"]')).toBeVisible({ timeout: 10000 })

        const countText = await page.locator('[data-testid="attr-count"]').textContent()
        const match = countText?.match(/\((\d+)\)/)

        expect(match).not.toBeNull()
        expect(parseInt(match![1])).toBeGreaterThan(0)
    })
})
