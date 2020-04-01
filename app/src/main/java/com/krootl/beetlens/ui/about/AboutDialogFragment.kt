package com.krootl.beetlens.ui.about

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.*
import android.text.Annotation
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.krootl.beetlens.R
import com.krootl.beetlens.ui.common.dpToPx
import kotlinx.android.synthetic.main.fragment_dialog_about.*


class AboutDialogFragment : DialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dialog_about, container, false)
    }

    override fun getTheme(): Int {
        return R.style.AboutDialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        buttonClose.setOnClickListener { dismiss() }
        buttonArticle.setOnClickListener { pleaseLaunchURL("https://medium.com/@shliama/beetlens-42eac15503d0") }
        buttonSources.setOnClickListener { pleaseLaunchURL("https://github.com/Krootl/Beetlens") }
        imageKrootl.setOnClickListener { pleaseLaunchURL("https://krootl.com") }
        textKrootlMotto.setOnClickListener { pleaseLaunchURL("https://krootl.com") }

        setupClickableDescription()

        startKrootlAnimation()
    }

    /**
     * It seems like there are infinite options to make clickable text in Android.
     * So I decided to try yet another one, using [android.text.Annotation].
     */
    private fun setupClickableDescription() {
        val fullText = getText(R.string.dialog_about_description) as SpannedString
        val spannableString = SpannableString(fullText)

        val annotations = fullText.getSpans(0, fullText.length, Annotation::class.java)

        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                pleaseLaunchURL("https://github.com/shliama")
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
                ds.color = ContextCompat.getColor(requireContext(), R.color.primaryColor)
            }
        }
        annotations?.find { it.value == "name_link" }?.let {
            spannableString.setSpan(clickableSpan, fullText.getSpanStart(it), fullText.getSpanEnd(it), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        textDescription.apply {
            text = spannableString
            highlightColor = Color.TRANSPARENT
            movementMethod = LinkMovementMethod()
        }
    }

    private fun startKrootlAnimation() {
        val startAnimationDelay = 300L
        val logoAnimationDuration = 500L
        val mottoAnimationDuration = 300L

        imageKrootl.apply {
            translationY = 25f.dpToPx()
            scaleX = 0.85f
            scaleY = 0.85f
            alpha = 0f
            animate()
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setStartDelay(startAnimationDelay)
                .setDuration(logoAnimationDuration)
                .interpolator = OvershootInterpolator()

        }

        textKrootlMotto.apply {
            scaleX = 0.85f
            scaleY = 0.85f
            alpha = 0f
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setStartDelay(startAnimationDelay + logoAnimationDuration)
                .setDuration(mottoAnimationDuration)
                .interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun pleaseLaunchURL(url: String) {
        val webPage = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, webPage)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Log.e(TAG, "Could not open web page — ($webPage)")
            Toast.makeText(requireContext(), "Could not find a web browser", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private val TAG = AboutDialogFragment::class.java.simpleName
    }
}