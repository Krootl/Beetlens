package com.krootl.beetlens.ui.common

import androidx.recyclerview.widget.RecyclerView


abstract class InfinitePagerAdapter<T, VH : RecyclerView.ViewHolder>(private val data: List<T>) : RecyclerView.Adapter<VH>() {

    override fun getItemCount(): Int = if (data.isEmpty()) 0 else data.size + 2

    fun getRealCount() = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val modelPosition = mapPagerPositionToModelPosition(position)
        val item: T = data[modelPosition]
        onBindViewHolder(holder, item)
    }

    abstract fun onBindViewHolder(holder: VH, data: T)

    private fun mapPagerPositionToModelPosition(pagerPosition: Int): Int {
        // Put last page model to the first position.
        if (pagerPosition == 0) {
            return getRealCount() - 1
        }
        // Put first page model to the last position.
        return if (pagerPosition == getRealCount() + 1) 0 else pagerPosition - 1
    }
}