package com.auto.app.ui.permission

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.auto.app.R
import com.auto.app.databinding.ViewPermissionItemBinding

class PermissionItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private val binding: ViewPermissionItemBinding
    
    init {
        val view = LayoutInflater.from(context).inflate(R.layout.view_permission_item, this, true)
        binding = ViewPermissionItemBinding.bind(view)
    }
    
    fun setTitle(title: String) {
        binding.tvTitle.text = title
    }
    
    fun setDescription(description: String) {
        binding.tvDescription.text = description
    }
    
    fun setGranted(granted: Boolean) {
        binding.ivStatus.setImageResource(
            if (granted) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
        )
        binding.ivStatus.setColorFilter(
            context.getColor(if (granted) R.color.success else R.color.text_hint)
        )
    }
}
