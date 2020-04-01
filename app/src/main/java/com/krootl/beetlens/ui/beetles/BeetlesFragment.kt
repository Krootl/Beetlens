package com.krootl.beetlens.ui.beetles

import android.animation.AnimatorInflater
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import com.krootl.beetlens.R
import com.krootl.beetlens.data.models.Beetle
import com.krootl.beetlens.ui.beetles.fab.FabIcon
import com.krootl.beetlens.ui.common.doOnApplyWindowInsets
import com.krootl.beetlens.ui.common.dpToPx
import kotlinx.android.synthetic.main.fragment_beetles.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign


class BeetlesFragment : Fragment(R.layout.fragment_beetles) {

    // Horizontal offset needed to change the page.
    private val minXOffsetChangePage = 60.dpToPx()

    // Vertical offset - boundaries within which it's allowed to change the page.
    private val maxYOffsetChangePage = 40.dpToPx()

    // Vertical offset - drag at least this distance to start Lens expansion animation.
    private val lensMinDistanceToStartExpanding = 60f.dpToPx()

    // Distance for the Lens expansion animation.
    private val lensDistanceToExpand = 60f.dpToPx()

    // Distance when the Lens is kinda "actively" used; use to perform UI and/or haptic feedback.
    private val lensActiveDistance = lensMinDistanceToStartExpanding + lensDistanceToExpand * 0.5

    private var appBarRevealed = false
    private var changePageIsOnCooldown = false
    private var isFabDragged = false
    private var lensIsExpanding = false
    private var lensIsActive = false

    private val canChangePage: Boolean
        get() = !changePageIsOnCooldown

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.doOnApplyWindowInsets { _, insets, _, _ ->
            bodyContainer.updatePadding(bottom = insets.systemWindowInsetBottom)
        }
        buttonAbout.setOnClickListener {
            findNavController().navigate(R.id.actionShowAboutDialogFragment)
        }

        setupViewPager()
        setupFab()

        setupLaunchAnimation()
    }

    private fun setupLaunchAnimation() {
        buttonAbout.isInvisible = true
        appBar.alpha = 0f
        appBar.stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_elevation_off)
        viewPager.alpha = 0f
        viewPager.post {
            val interpolator = AccelerateDecelerateInterpolator()
            val duration = 500L
            viewPager.animate()
                .setInterpolator(interpolator)
                .setDuration(duration)
                .alpha(1f)

            appBar.animate()
                .setInterpolator(interpolator)
                .setDuration(duration)
                .alpha(1f)
        }

        metaballsFab.hideButton(animate = false)
        metaballsFab.userTouchEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            // Let the base UI to fade in and then reveal the FAB with animation.
            delay(1000)
            metaballsFab.showButton()
            // Wait and reveal the app bar and hint icons around the FAB.
            delay(1000)
            revealAppBar()
            metaballsFab.revealOnboardingHints()
            // Wait and show the FAB animated onboarding, then enable user interactions.
            delay(1000)
            metaballsFab.fakeDragBy(dY = (-60f).dpToPx())
            metaballsFab.userTouchEnabled = true
        }
    }

    private fun setupViewPager() {
        val beetlesAdapter = BeetlesAdapter(BEETLES)
        viewPager.apply {
            isUserInputEnabled = true
            offscreenPageLimit = 1
            adapter = beetlesAdapter
            registerOnPageChangeCallback(InfinitePageChangeCallback(beetlesAdapter))
            setCurrentItem(1, false)
        }
    }

    private fun setupFab() {
        metaballsFab.onDragStateChanged = { isFabDragged ->
            this.isFabDragged = isFabDragged
            if (!isFabDragged) metaballsFab.setFabIcon(FabIcon.Beetle())
        }
        metaballsFab.onPositionChanged = { center: PointF, positionOffset: PointF ->
            if (isAdded) {
                updatePage(positionOffset)
                updateFabIcon(positionOffset)
                updateLens(center, positionOffset)
            }
        }
    }

    /**
     * Change ViewPager current item as soon as the FAB is dragged far enough left or right.
     */
    private fun updatePage(positionOffset: PointF) {
        if (isFabDragged && canChangePage) {
            if (positionOffset.x.absoluteValue > minXOffsetChangePage && positionOffset.y.absoluteValue < maxYOffsetChangePage) {
                changePageIsOnCooldown = true
                lifecycleScope.launch {
                    delay(600)
                    changePageIsOnCooldown = false
                }

                val nextPage = viewPager.currentItem + sign(positionOffset.x).toInt()
                viewPager.setCurrentItem(nextPage, true)
                metaballsFab.performHapticFeedback()

                metaballsFab.hideOnboardingHint(right = positionOffset.x > 0, left = positionOffset.x < 0)
            }
        }
    }

    /**
     * Provide some visual hint depending on the FAB position.
     */
    private fun updateFabIcon(positionOffset: PointF) {
        if (positionOffset.y < -lensActiveDistance) {
            metaballsFab.setFabIcon(FabIcon.Empty)
        } else if (positionOffset.y < -(minXOffsetChangePage / 2)) {
            metaballsFab.setFabIcon(FabIcon.ArrowUp())
        } else if (positionOffset.x.absoluteValue > minXOffsetChangePage / 2 && positionOffset.y.absoluteValue < maxYOffsetChangePage) {
            if (positionOffset.x > 0) metaballsFab.setFabIcon(FabIcon.ArrowRight())
            else metaballsFab.setFabIcon(FabIcon.ArrowLeft())
        } else {
            metaballsFab.setFabIcon(FabIcon.Beetle())
        }
    }

    /**
     * Expand/collapse the LENS depending on the FAB position, whether it is dragged upwards enough.
     */
    private fun updateLens(metaballsFabCenter: PointF, metaballsFabPositionOffset: PointF) {
        val metaballsFabOffsetY = max(-metaballsFabPositionOffset.y, 0f)
        val canBeExpanded = metaballsFabOffsetY > lensMinDistanceToStartExpanding

        if (this.lensIsExpanding || canBeExpanded) {
            val expandFraction = when {
                canBeExpanded -> min((metaballsFabOffsetY - lensMinDistanceToStartExpanding) / lensDistanceToExpand, 1f)
                else -> 0f
            }
            lens.updateLens(fabCenter = metaballsFabCenter, expandFraction = expandFraction)
        }

        if (this.lensIsExpanding != canBeExpanded) {
            this.lensIsExpanding = canBeExpanded
            glFrameLayout.setViewToGLRenderer(if (canBeExpanded) lens.renderer else null)
        }

        val lensIsActive = metaballsFabOffsetY > lensActiveDistance
        if (this.lensIsActive != lensIsActive) {
            this.lensIsActive = lensIsActive

            metaballsFab.drawFabMetaball = !lensIsActive
            metaballsFab.performHapticFeedback()
            metaballsFab.hideOnboardingHint(up = true)
            metaballsFab.setSlotIcon(icon = if (lensIsActive) FabIcon.Close() else FabIcon.Empty)
        }
    }

    private fun revealAppBar() {
        if (appBarRevealed) return
        appBarRevealed = true

        appBar.stateListAnimator = AnimatorInflater.loadStateListAnimator(context, R.animator.appbar_elevation_on)

        buttonAbout.apply {
            alpha = 0f
            isInvisible = false
            scaleX = 0.75f
            scaleY = 0.75f

            animate()
                .setInterpolator(AccelerateDecelerateInterpolator())
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f).duration = 500
        }
    }

    inner class InfinitePageChangeCallback(private val adapter: BeetlesAdapter) : ViewPager2.OnPageChangeCallback() {

        private var jumpPosition = -1

        // Prepare to jump start<->end
        override fun onPageSelected(position: Int) {
            if (position == 0) {
                jumpPosition = adapter.getRealCount()
            } else if (position == adapter.getRealCount() + 1) {
                jumpPosition = 1
            }
        }

        // Wait for the animation to complete then do the jump
        override fun onPageScrollStateChanged(state: Int) {
            if (jumpPosition >= 0 && state == ViewPager.SCROLL_STATE_IDLE) {
                viewPager.setCurrentItem(jumpPosition, false)
                jumpPosition = -1
            }
        }
    }
}

private val BEETLES = listOf(
    Beetle(R.drawable.beetle_001),
    Beetle(R.drawable.beetle_002),
    Beetle(R.drawable.beetle_003),
    Beetle(R.drawable.beetle_004),
    Beetle(R.drawable.beetle_005),
    Beetle(R.drawable.beetle_006),
    Beetle(R.drawable.beetle_007),
    Beetle(R.drawable.beetle_008),
    Beetle(R.drawable.beetle_009),
    Beetle(R.drawable.beetle_010),
    Beetle(R.drawable.beetle_011),
    Beetle(R.drawable.beetle_012),
    Beetle(R.drawable.beetle_014),
    Beetle(R.drawable.beetle_015),
    Beetle(R.drawable.beetle_016),
    Beetle(R.drawable.beetle_017),
    Beetle(R.drawable.beetle_018),
    Beetle(R.drawable.beetle_019),
    Beetle(R.drawable.beetle_020),
    Beetle(R.drawable.beetle_021),
    Beetle(R.drawable.beetle_022)
)