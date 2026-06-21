package com.jaaspass.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Ponto único de verdade para a aparência do app (tarefa "ui-appearance"): paleta escura,
 * tokens de espaçamento/raio e fábricas de widgets estilizados. Toda a estilização é
 * 100% programática (constraint: zero deps de terceiros — apenas `android.*`).
 *
 * As Activities montam suas telas a partir destas fábricas para garantir consistência
 * visual e evitar estilo duplicado.
 */
object Theme {

    // --- Paleta escura (ARGB) -------------------------------------------------
    val windowBg = Color.parseColor("#121317")     // fundo da janela
    val surface = Color.parseColor("#1E2026")      // cartão central
    val surfaceInput = Color.parseColor("#272A32") // fundo dos inputs
    val accent = Color.parseColor("#4C8DFF")       // cor primária / botões
    val accentText = Color.parseColor("#0B1220")   // texto sobre o acento
    val onBackground = Color.parseColor("#ECEDEF") // texto principal
    val onSurfaceMuted = Color.parseColor("#A6ABB5") // texto secundário
    val hintColor = Color.parseColor("#6C7280")    // placeholder/hint
    val danger = Color.parseColor("#FF5D5D")       // ações destrutivas

    // --- Tokens de layout -----------------------------------------------------
    const val SPACE = 12           // unidade base de espaçamento (dp)
    const val RADIUS = 14          // raio de canto (dp)
    const val CARD_MAX_WIDTH = 480 // largura máxima do conteúdo (dp)

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private fun roundedBg(context: Context, fill: Int, stroke: Int? = null) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dp(context, RADIUS).toFloat()
        setColor(fill)
        if (stroke != null) setStroke(dp(context, 1), stroke)
    }

    private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    /**
     * Largura responsiva do cartão: preenche a tela (menos o padding) no celular e só é
     * limitada a [CARD_MAX_WIDTH] em telas largas (tablet). Evita corte lateral.
     */
    private fun cardWidth(context: Context): Int {
        val available = context.resources.displayMetrics.widthPixels - 2 * dp(context, SPACE * 2)
        val maxPx = dp(context, CARD_MAX_WIDTH)
        return if (available <= maxPx) MATCH else maxPx
    }

    /**
     * Tela rolável (forms): fundo escuro + [content] centralizado dentro de um `ScrollView`,
     * para que conteúdo mais alto que a tela role em vez de cortar. Use em telas de formulário.
     */
    fun screen(context: Context, content: View): ScrollView {
        val holder = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            val pad = dp(context, SPACE * 2)
            setPadding(pad, pad, pad, pad)
            layoutParams = ViewGroup.LayoutParams(MATCH, WRAP)
            addView(content)
        }
        return ScrollView(context).apply {
            setBackgroundColor(windowBg)
            isFillViewport = true
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
            addView(holder)
        }
    }

    /**
     * Tela estática de altura cheia (ex.: lista que rola internamente). [content] é adicionado
     * a um container que ocupa toda a tela; o próprio conteúdo gerencia sua rolagem.
     */
    fun staticScreen(context: Context, content: View): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(windowBg)
        val pad = dp(context, SPACE * 2)
        setPadding(pad, pad, pad, pad)
        layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        addView(content)
    }

    /** Cartão arredondado, com largura responsiva, onde o conteúdo da tela é montado. */
    fun card(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = roundedBg(context, surface)
        val pad = dp(context, SPACE + SPACE / 2)
        setPadding(pad, pad, pad, pad)
        layoutParams = LinearLayout.LayoutParams(cardWidth(context), WRAP)
    }

    fun titleText(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(onBackground)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 0, 0, dp(context, SPACE))
    }

    fun bodyText(context: Context, text: String, muted: Boolean = false): TextView = TextView(context).apply {
        this.text = text
        setTextColor(if (muted) onSurfaceMuted else onBackground)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setPadding(0, 0, 0, dp(context, SPACE))
    }

    private fun stylize(context: Context, edit: EditText, hintText: String, type: Int) = edit.apply {
        hint = hintText
        inputType = type
        setTextColor(onBackground)
        setHintTextColor(hintColor)
        background = roundedBg(context, surfaceInput)
        val padH = dp(context, SPACE + SPACE / 4)
        val padV = dp(context, SPACE)
        setPadding(padH, padV, padH, padV)
    }

    /** EditText estilizado para campos comuns (rótulo, usuário...). */
    fun input(context: Context, hintText: String, type: Int): EditText =
        stylize(context, EditText(context), hintText, type).apply {
            layoutParams = spacedRow(context)
        }

    private fun primaryStyle(context: Context, button: Button) = button.apply {
        background = roundedBg(context, accent)
        setTextColor(accentText)
        isAllCaps = false
        val padV = dp(context, SPACE / 2)
        setPadding(0, padV, 0, padV)
    }

    fun primaryButton(context: Context, text: String): Button = Button(context).apply {
        this.text = text
        primaryStyle(context, this)
        layoutParams = spacedRow(context)
    }

    fun secondaryButton(context: Context, text: String, destructive: Boolean = false): Button = Button(context).apply {
        this.text = text
        isAllCaps = false
        background = roundedBg(context, surfaceInput, stroke = if (destructive) danger else accent)
        setTextColor(if (destructive) danger else accent)
        val padV = dp(context, SPACE / 2)
        setPadding(0, padV, 0, padV)
        layoutParams = spacedRow(context)
    }

    private fun spacedRow(context: Context) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(context, SPACE) }

    /**
     * Campo de senha com botão de olho (mostrar/ocultar) acoplado. Inicia **mascarado**.
     *
     * Retorna o [PasswordField], que expõe o [EditText] interno para leitura/extração
     * (ex.: `extractChars()`), sem expor o controle de toggle.
     */
    fun passwordField(context: Context, hintText: String): PasswordField {
        val edit = stylize(
            context, EditText(context), hintText,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val toggle = Button(context).apply {
            text = "Mostrar"
            isAllCaps = false
            background = roundedBg(context, surfaceInput, stroke = accent)
            setTextColor(accent)
            val padH = dp(context, SPACE / 2)
            setPadding(padH, 0, padH, 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply { leftMargin = dp(context, SPACE / 2) }
        }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(edit)
            addView(toggle)
            layoutParams = spacedRow(context)
        }
        return PasswordField(row, edit, toggle)
    }
}

/**
 * Campo de senha com olho. [view] é adicionada ao layout; [edit] é o input para leitura.
 * O toggle alterna a visibilidade preservando o cursor e a fonte (tarefa "password-visibility").
 */
class PasswordField(
    val view: LinearLayout,
    val edit: EditText,
    private val toggle: Button,
) {
    private var revealed = false

    init {
        toggle.setOnClickListener { setRevealed(!revealed) }
    }

    private fun setRevealed(reveal: Boolean) {
        revealed = reveal
        // Salvar seleção e fonte: trocar inputType reseta o cursor e pode forçar fonte monoespaçada.
        val start = edit.selectionStart
        val end = edit.selectionEnd
        val tf = edit.typeface
        edit.inputType = passwordInputType(reveal)
        edit.typeface = tf
        val len = edit.length()
        edit.setSelection(clampSelection(start, len), clampSelection(end, len))
        toggle.text = if (reveal) "Ocultar" else "Mostrar"
    }

    companion object {
        /** inputType para o estado [reveal] (visível vs. mascarado). Função pura — testável. */
        fun passwordInputType(reveal: Boolean): Int =
            InputType.TYPE_CLASS_TEXT or
                if (reveal) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                else InputType.TYPE_TEXT_VARIATION_PASSWORD

        /** Garante que a posição do cursor restaurada caia dentro de `0..length`. Função pura. */
        fun clampSelection(pos: Int, length: Int): Int = pos.coerceIn(0, length)
    }
}
