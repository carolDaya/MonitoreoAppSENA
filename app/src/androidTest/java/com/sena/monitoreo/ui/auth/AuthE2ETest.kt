package com.sena.monitoreo.e2e

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sena.monitoreo.R
import com.sena.monitoreo.ui.auth.LoginActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthE2ETest {

    // Datos de prueba
    private val testPhone = "3112345678"
    private val testPassword = "password123"
    private val testName = "Test User"

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Before
    fun setUp() {
        // Limpiar SharedPreferences antes de cada prueba
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        // Cerrar todas las actividades
        activityRule.scenario.close()
    }

    @Test
    fun testLoginSuccess_AdminUser() {
        // Arrange: Ir a Login si no estamos allí

        // Act: Ingresar credenciales y hacer login
        Espresso.onView(ViewMatchers.withId(R.id.input_phone))
            .perform(ViewActions.typeText("1234567890")) // Admin test phone
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_password))
            .perform(ViewActions.typeText("admin123"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.login_button))
            .perform(ViewActions.click())

    }

    @Test
    fun testLoginSuccess_RegularUser() {


        Espresso.onView(ViewMatchers.withId(R.id.input_phone))
            .perform(ViewActions.typeText(testPhone))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_password))
            .perform(ViewActions.typeText(testPassword))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.login_button))
            .perform(ViewActions.click())

        Thread.sleep(3000)

    }

    @Test
    fun testLoginInvalidCredentials() {
        Espresso.onView(ViewMatchers.withId(R.id.input_phone))
            .perform(ViewActions.typeText("0000000000"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_password))
            .perform(ViewActions.typeText("wrongpass"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.login_button))
            .perform(ViewActions.click())

        // Verificar que mostramos mensaje de error
        Thread.sleep(2000)
        Espresso.onView(ViewMatchers.withId(com.google.android.material.R.id.snackbar_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testSignupFlow() {

        // Ir a Signup
        Espresso.onView(ViewMatchers.withId(R.id.create_account_text))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Completar formulario de registro
        Espresso.onView(ViewMatchers.withId(R.id.edit_text_name))
            .perform(ViewActions.typeText(testName))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.edit_text_phone))
            .perform(ViewActions.typeText("9998887777"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_password))
            .perform(ViewActions.typeText(testPassword))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_confirm_password))
            .perform(ViewActions.typeText(testPassword))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.button_register))
            .perform(ViewActions.click())

        Thread.sleep(3000)
    }

    @Test
    fun testForgotPasswordFlow() {

        // Ir a Forgot Password
        Espresso.onView(ViewMatchers.withId(R.id.forgot_password_text))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Ingresar teléfono
        Espresso.onView(ViewMatchers.withId(R.id.edit_text_phone))
            .perform(ViewActions.typeText(testPhone))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.button_reset_password))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Verificar que estamos en ResetPasswordActivity
        Espresso.onView(ViewMatchers.withId(R.id.container_reset))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testResetPasswordFlow() {
        // Asumimos que ya estamos en ResetPasswordActivity (continuación del flujo anterior)
        testForgotPasswordFlow()

        // Ingresar nueva contraseña
        Espresso.onView(ViewMatchers.withId(R.id.input_new_password))
            .perform(ViewActions.typeText("newpassword123"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_confirm_new_password))
            .perform(ViewActions.typeText("newpassword123"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.button_set_new_password))
            .perform(ViewActions.click())

        Thread.sleep(2000)

        // Verificar que volvemos a Login
        Espresso.onView(ViewMatchers.withId(R.id.container_login))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testEmptyFieldsValidation() {

        // Intentar login con campos vacíos
        Espresso.onView(ViewMatchers.withId(R.id.login_button))
            .perform(ViewActions.click())

        // Verificar que se muestra mensaje de validación
        Espresso.onView(ViewMatchers.withText("Por favor completa todos los campos"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testSignupValidation() {
        // Ir a Signup
        Espresso.onView(ViewMatchers.withId(R.id.create_account_text))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Intentar registro con contraseñas que no coinciden
        Espresso.onView(ViewMatchers.withId(R.id.edit_text_name))
            .perform(ViewActions.typeText(testName))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.edit_text_phone))
            .perform(ViewActions.typeText(testPhone))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_password))
            .perform(ViewActions.typeText("password123"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.input_confirm_password))
            .perform(ViewActions.typeText("differentpass"))
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.button_register))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Verificar mensaje de error
        Espresso.onView(ViewMatchers.withText("Las contraseñas no coinciden"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testPhoneFormatValidation() {
        // Ir a Forgot Password
        Espresso.onView(ViewMatchers.withId(R.id.forgot_password_text))
            .perform(ViewActions.click())

        Thread.sleep(1000)

        // Ingresar teléfono inválido
        Espresso.onView(ViewMatchers.withId(R.id.edit_text_phone))
            .perform(ViewActions.typeText("123")) // Muy corto
            .perform(ViewActions.closeSoftKeyboard())

        Espresso.onView(ViewMatchers.withId(R.id.button_reset_password))
            .perform(ViewActions.click())

        // Verificar mensaje de validación
        Espresso.onView(ViewMatchers.withText("El teléfono debe tener 10 dígitos"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testNavigationBetweenAuthScreens() {
        // Login -> Signup
        Espresso.onView(ViewMatchers.withId(R.id.create_account_text))
            .perform(ViewActions.click())

        Thread.sleep(500)
        Espresso.onView(ViewMatchers.withId(R.id.container_signup))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Signup -> Login (volver)
        Espresso.pressBack()

        Thread.sleep(500)
        Espresso.onView(ViewMatchers.withId(R.id.container_login))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Login -> Forgot Password
        Espresso.onView(ViewMatchers.withId(R.id.forgot_password_text))
            .perform(ViewActions.click())

        Thread.sleep(500)
        Espresso.onView(ViewMatchers.withId(R.id.container_forgot))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }


}