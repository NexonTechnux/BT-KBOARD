package com.example

object HidKeys {
    val KEY_A = 0x04.toByte()
    val KEY_B = 0x05.toByte()
    val KEY_C = 0x06.toByte()
    val KEY_D = 0x07.toByte()
    val KEY_E = 0x08.toByte()
    val KEY_F = 0x09.toByte()
    val KEY_G = 0x0A.toByte()
    val KEY_H = 0x0B.toByte()
    val KEY_I = 0x0C.toByte()
    val KEY_J = 0x0D.toByte()
    val KEY_K = 0x0E.toByte()
    val KEY_L = 0x0F.toByte()
    val KEY_M = 0x10.toByte()
    val KEY_N = 0x11.toByte()
    val KEY_O = 0x12.toByte()
    val KEY_P = 0x13.toByte()
    val KEY_Q = 0x14.toByte()
    val KEY_R = 0x15.toByte()
    val KEY_S = 0x16.toByte()
    val KEY_T = 0x17.toByte()
    val KEY_U = 0x18.toByte()
    val KEY_V = 0x19.toByte()
    val KEY_W = 0x1A.toByte()
    val KEY_X = 0x1B.toByte()
    val KEY_Y = 0x1C.toByte()
    val KEY_Z = 0x1D.toByte()
    
    val KEY_1 = 0x1E.toByte()
    val KEY_2 = 0x1F.toByte()
    val KEY_3 = 0x20.toByte()
    val KEY_4 = 0x21.toByte()
    val KEY_5 = 0x22.toByte()
    val KEY_6 = 0x23.toByte()
    val KEY_7 = 0x24.toByte()
    val KEY_8 = 0x25.toByte()
    val KEY_9 = 0x26.toByte()
    val KEY_0 = 0x27.toByte()
    
    val KEY_ENTER = 0x28.toByte()
    val KEY_ESC = 0x29.toByte()
    val KEY_BACKSPACE = 0x2A.toByte()
    val KEY_TAB = 0x2B.toByte()
    val KEY_SPACE = 0x2C.toByte()
    
    val KEY_MINUS = 0x2D.toByte()     // - and _
    val KEY_EQUAL = 0x2E.toByte()     // = and +
    val KEY_LEFT_BRACKET = 0x2F.toByte()  // [ and {
    val KEY_RIGHT_BRACKET = 0x30.toByte()  // ] and }
    val KEY_BACKSLASH = 0x31.toByte()     // \ and |
    val KEY_SEMICOLON = 0x33.toByte()     // ; and :
    val KEY_APOSTROPHE = 0x34.toByte()    // ' and "
    val KEY_GRAVE = 0x35.toByte()         // ` and ~
    val KEY_COMMA = 0x36.toByte()         // , and <
    val KEY_PERIOD = 0x37.toByte()        // . and >
    val KEY_SLASH = 0x38.toByte()         // / and ?
    val KEY_CAPS_LOCK = 0x39.toByte()
    
    val KEY_RIGHT_ARROW = 0x4F.toByte()
    val KEY_LEFT_ARROW = 0x50.toByte()
    val KEY_DOWN_ARROW = 0x51.toByte()
    val KEY_UP_ARROW = 0x52.toByte()
    
    val KEY_F1 = 0x3A.toByte()
    val KEY_F2 = 0x3B.toByte()
    val KEY_F3 = 0x3C.toByte()
    val KEY_F4 = 0x3D.toByte()
    val KEY_F5 = 0x3E.toByte()
    val KEY_F6 = 0x3F.toByte()
    val KEY_F7 = 0x40.toByte()
    val KEY_F8 = 0x41.toByte()
    val KEY_F9 = 0x42.toByte()
    val KEY_F10 = 0x43.toByte()
    val KEY_F11 = 0x44.toByte()
    val KEY_F12 = 0x45.toByte()
    
    val KEY_DELETE = 0x4C.toByte()
    val KEY_HOME = 0x4A.toByte()
    val KEY_END = 0x4D.toByte()
    val KEY_PAGE_UP = 0x4B.toByte()
    val KEY_PAGE_DOWN = 0x4E.toByte()
}

object HidModifiers {
    val MODIFIER_LEFT_CTRL = 0x01.toByte()
    val MODIFIER_LEFT_SHIFT = 0x02.toByte()
    val MODIFIER_LEFT_ALT = 0x04.toByte()
    val MODIFIER_LEFT_GUI = 0x08.toByte() // Tasto Windows
    val MODIFIER_RIGHT_CTRL = 0x10.toByte()
    val MODIFIER_RIGHT_SHIFT = 0x20.toByte()
    val MODIFIER_RIGHT_ALT = 0x40.toByte() // AltGr
    val MODIFIER_RIGHT_GUI = 0x80.toByte()
}

data class KeyItem(
    val label: String,
    val shiftLabel: String = "",
    val code: Byte = 0,
    val isModifier: Boolean = false,
    val isSticky: Boolean = false,
    val modifierBit: Byte = 0,
    val flexWidth: Float = 1.0f
)

fun getKeyboardRows(): List<List<KeyItem>> {
    return listOf(
        // Riga 1: Tastierino Funzioni top
        listOf(
            KeyItem("Esc", code = HidKeys.KEY_ESC, flexWidth = 1.2f),
            KeyItem("F1", code = HidKeys.KEY_F1),
            KeyItem("F2", code = HidKeys.KEY_F2),
            KeyItem("F3", code = HidKeys.KEY_F3),
            KeyItem("F4", code = HidKeys.KEY_F4),
            KeyItem("F5", code = HidKeys.KEY_F5),
            KeyItem("F6", code = HidKeys.KEY_F6),
            KeyItem("F7", code = HidKeys.KEY_F7),
            KeyItem("F8", code = HidKeys.KEY_F8),
            KeyItem("F9", code = HidKeys.KEY_F9),
            KeyItem("F10", code = HidKeys.KEY_F10),
            KeyItem("F11", code = HidKeys.KEY_F11),
            KeyItem("F12", code = HidKeys.KEY_F12),
            KeyItem("Canc", code = HidKeys.KEY_DELETE, flexWidth = 1.2f)
        ),
        // Riga 2: Numeri e Simboli
        listOf(
            KeyItem("`", shiftLabel = "~", code = HidKeys.KEY_GRAVE),
            KeyItem("1", shiftLabel = "!", code = HidKeys.KEY_1),
            KeyItem("2", shiftLabel = "@", code = HidKeys.KEY_2),
            KeyItem("3", shiftLabel = "#", code = HidKeys.KEY_3),
            KeyItem("4", shiftLabel = "$", code = HidKeys.KEY_4),
            KeyItem("5", shiftLabel = "%", code = HidKeys.KEY_5),
            KeyItem("6", shiftLabel = "^", code = HidKeys.KEY_6),
            KeyItem("7", shiftLabel = "&", code = HidKeys.KEY_7),
            KeyItem("8", shiftLabel = "*", code = HidKeys.KEY_8),
            KeyItem("9", shiftLabel = "(", code = HidKeys.KEY_9),
            KeyItem("0", shiftLabel = ")", code = HidKeys.KEY_0),
            KeyItem("-", shiftLabel = "_", code = HidKeys.KEY_MINUS),
            KeyItem("=", shiftLabel = "+", code = HidKeys.KEY_EQUAL),
            KeyItem("Backspace", code = HidKeys.KEY_BACKSPACE, flexWidth = 2.1f)
        ),
        // Riga 3: QWERTY Row 1
        listOf(
            KeyItem("Tab", code = HidKeys.KEY_TAB, flexWidth = 1.6f),
            KeyItem("Q", code = HidKeys.KEY_Q),
            KeyItem("W", code = HidKeys.KEY_W),
            KeyItem("E", code = HidKeys.KEY_E),
            KeyItem("R", code = HidKeys.KEY_R),
            KeyItem("T", code = HidKeys.KEY_T),
            KeyItem("Y", code = HidKeys.KEY_Y),
            KeyItem("U", code = HidKeys.KEY_U),
            KeyItem("I", code = HidKeys.KEY_I),
            KeyItem("O", code = HidKeys.KEY_O),
            KeyItem("P", code = HidKeys.KEY_P),
            KeyItem("[", shiftLabel = "{", code = HidKeys.KEY_LEFT_BRACKET),
            KeyItem("]", shiftLabel = "}", code = HidKeys.KEY_RIGHT_BRACKET),
            KeyItem("\\", shiftLabel = "|", code = HidKeys.KEY_BACKSLASH, flexWidth = 1.5f)
        ),
        // Riga 4: QWERTY Row 2
        listOf(
            KeyItem("Caps", code = HidKeys.KEY_CAPS_LOCK, flexWidth = 1.9f),
            KeyItem("A", code = HidKeys.KEY_A),
            KeyItem("S", code = HidKeys.KEY_S),
            KeyItem("D", code = HidKeys.KEY_D),
            KeyItem("F", code = HidKeys.KEY_F),
            KeyItem("G", code = HidKeys.KEY_G),
            KeyItem("H", code = HidKeys.KEY_H),
            KeyItem("J", code = HidKeys.KEY_J),
            KeyItem("K", code = HidKeys.KEY_K),
            KeyItem("L", code = HidKeys.KEY_L),
            KeyItem(";", shiftLabel = ":", code = HidKeys.KEY_SEMICOLON),
            KeyItem("'", shiftLabel = "\"", code = HidKeys.KEY_APOSTROPHE),
            KeyItem("Enter", code = HidKeys.KEY_ENTER, flexWidth = 2.2f)
        ),
        // Riga 5: QWERTY Row 3
        listOf(
            KeyItem("Shift", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_LEFT_SHIFT, flexWidth = 2.4f),
            KeyItem("Z", code = HidKeys.KEY_Z),
            KeyItem("X", code = HidKeys.KEY_X),
            KeyItem("C", code = HidKeys.KEY_C),
            KeyItem("V", code = HidKeys.KEY_V),
            KeyItem("B", code = HidKeys.KEY_B),
            KeyItem("N", code = HidKeys.KEY_N),
            KeyItem("M", code = HidKeys.KEY_M),
            KeyItem(",", shiftLabel = "<", code = HidKeys.KEY_COMMA),
            KeyItem(".", shiftLabel = ">", code = HidKeys.KEY_PERIOD),
            KeyItem("/", shiftLabel = "?", code = HidKeys.KEY_SLASH),
            KeyItem("▲", code = HidKeys.KEY_UP_ARROW, flexWidth = 1.1f),
            KeyItem("Shift", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_RIGHT_SHIFT, flexWidth = 2.0f)
        ),
        // Riga 6: Modificatori inferiori e Spazio
        listOf(
            KeyItem("Ctrl", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_LEFT_CTRL, flexWidth = 1.5f),
            KeyItem("Win", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_LEFT_GUI, flexWidth = 1.3f),
            KeyItem("Alt", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_LEFT_ALT, flexWidth = 1.4f),
            KeyItem("Space", code = HidKeys.KEY_SPACE, flexWidth = 6.2f),
            KeyItem("AltGr", isModifier = true, isSticky = true, modifierBit = HidModifiers.MODIFIER_RIGHT_ALT, flexWidth = 1.4f),
            KeyItem("◀", code = HidKeys.KEY_LEFT_ARROW),
            KeyItem("▼", code = HidKeys.KEY_DOWN_ARROW),
            KeyItem("▶", code = HidKeys.KEY_RIGHT_ARROW)
        )
    )
}
