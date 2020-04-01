package com.krootl.beetlens.ui.beetles

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.CENTER_INSIDE
import com.krootl.beetlens.R
import com.krootl.beetlens.data.models.Beetle
import com.krootl.beetlens.ui.common.InfinitePagerAdapter
import kotlinx.android.synthetic.main.layout_beetle.view.*


class BeetlesAdapter(list: List<Beetle>) : InfinitePagerAdapter<Beetle, BeetleViewHolder>(list) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeetleViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = inflater.inflate(R.layout.layout_beetle, parent, false)
        return BeetleViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: BeetleViewHolder, data: Beetle) {
        holder.bind(data)
    }
}

class BeetleViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(beetle: Beetle) {
        Glide
            .with(itemView)
            .load(beetle.image)
            .downsample(CENTER_INSIDE)
            .centerInside()
            .into(itemView.imageBeetle)
    }
}
