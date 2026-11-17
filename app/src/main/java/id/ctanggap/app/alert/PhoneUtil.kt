package id.ctanggap.app.alert

object PhoneUtil {
    /** Dua nomor dianggap sama jika 9 digit terakhirnya sama (menyamakan +62 / 0 / 62). */
    fun same(a: String, b: String): Boolean {
        val x = a.filter { it.isDigit() }.takeLast(9)
        val y = b.filter { it.isDigit() }.takeLast(9)
        return x.length >= 7 && x == y
    }
}
