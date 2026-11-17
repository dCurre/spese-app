package com.dcapps.spese.core.prefs

/**
 * Un criterio di ordinamento attivo: la chiave del campo + la sua direzione. Una lista di
 * [SortCriterion] (ordine = priorità) descrive un ordinamento MULTI-CAMPO a cascata: il primo è il
 * criterio primario, i successivi sono tie-breaker. Ogni criterio ha la sua direzione indipendente.
 */
data class SortCriterion<K>(val key: K, val ascending: Boolean)

/**
 * Macchina a stati del tap su un chip (ciclo a 3 stati), condivisa da tutte le schermate così il
 * comportamento è identico ovunque:
 * - 1° tap (criterio assente): lo aggiunge in coda alla cascata, ascending;
 * - 2° tap (presente, ascending): ne inverte la direzione (descending);
 * - 3° tap (presente, descending): lo rimuove dalla cascata.
 */
fun <K> List<SortCriterion<K>>.toggled(key: K): List<SortCriterion<K>> = when {
    none { it.key == key } -> this + SortCriterion(key, ascending = true)
    first { it.key == key }.ascending ->
        map { if (it.key == key) it.copy(ascending = false) else it }
    else -> filterNot { it.key == key }
}

/**
 * Costruisce un [Comparator] concatenando i criteri nell'ordine della cascata (primario → tie-break),
 * applicando la direzione di ciascuno. Ritorna null se la lista è vuota: in quel caso il chiamante
 * usa il proprio ordinamento di default.
 *
 * [selector] mappa la chiave del campo all'estrattore del valore confrontabile per l'elemento T.
 */
@Suppress("UNCHECKED_CAST")
fun <T, K> List<SortCriterion<K>>.comparator(
    selector: (K) -> (T) -> Comparable<*>?,
): Comparator<T>? {
    if (isEmpty()) return null
    var cmp: Comparator<T>? = null
    for (criterion in this) {
        val extractor = selector(criterion.key)
        val base: Comparator<T> = compareBy(nullsFirst()) { extractor(it) as Comparable<Any>? }
        val directed = if (criterion.ascending) base else base.reversed()
        cmp = cmp?.then(directed) ?: directed
    }
    return cmp
}

/**
 * Converte i criteri salvati ([SortPreferences.Saved]) in [SortCriterion], mappando il nome del
 * criterio alla chiave enum via [parse] (ritorna null per nomi non più validi, che vengono scartati).
 */
fun <K> List<SortPreferences.Saved>.toSortCriteria(parse: (String) -> K?): List<SortCriterion<K>> =
    mapNotNull { saved -> parse(saved.criterion)?.let { SortCriterion(it, saved.ascending) } }

/** Converte la cascata in forma serializzabile per [SortPreferences.setList]. */
fun <K> List<SortCriterion<K>>.toSaved(): List<SortPreferences.Saved> =
    map { SortPreferences.Saved((it.key as Enum<*>).name, it.ascending) }
