package com.heavencanthandle.lakewords.intro

/**
 * The tagline's page, on a cleared screen: the tagline, then the pieces under it come in one at
 * a time ([showNext]). Each piece only adds black, so on e-ink the ones before stay crisp and
 * whatever the panel still holds of the shore stays as a ghost under them.
 *
 * [parts] are masks [width] x [height], true where ink is, in the order they come: the tagline
 * first.
 */
class TaglinePage(
    val width: Int,
    val height: Int,
    private val parts: List<BooleanArray>,
) {
    var shown = 1
        private set

    val finished: Boolean get() = shown >= parts.size

    fun showNext() {
        if (!finished) shown++
    }

    fun render(into: BooleanArray) {
        into.fill(false)
        for (k in 0 until shown) {
            val part = parts[k]
            for (i in into.indices) if (part[i]) into[i] = true
        }
    }
}
