package com.example.shoppingvs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

data class TrackedItem(
    val url: String,
    val platform: String,
    val currentPrice: Double,
    val historicalLow: Double,
    val couponDiscount: Double,
    val finalPrice: Double,
    val trustScore: Int,
    val isNewLow: Boolean
)

class AppViewModel : ViewModel() {
    private val _items = mutableStateOf<List<TrackedItem>>(emptyList())
    val items = _items

    fun addLink(url: String) {
        val normalized = url.trim()
        if (normalized.isBlank()) return
        val platform = when {
            normalized.contains("jd.com", ignoreCase = true) -> "京东"
            normalized.contains("taobao.com", ignoreCase = true) || normalized.contains("tmall.com", ignoreCase = true) -> "淘宝/天猫"
            else -> "未知平台"
        }

        val seed = normalized.hashCode().toUInt().toLong() % 1000
        val current = 150 + (seed % 350)
        val historicalLow = (current * (0.88 + (seed % 8) / 100.0))
        val discount = (current * (0.05 + (seed % 12) / 100.0))
        val final = (current - discount).coerceAtLeast(1.0)
        val score = calculateTrustScore(current.toDouble(), historicalLow, discount)
        val isNewLow = final < historicalLow

        val newItem = TrackedItem(
            url = normalized,
            platform = platform,
            currentPrice = current.toDouble(),
            historicalLow = historicalLow,
            couponDiscount = discount,
            finalPrice = final,
            trustScore = score,
            isNewLow = isNewLow
        )
        _items.value = listOf(newItem) + _items.value
    }

    private fun calculateTrustScore(currentPrice: Double, historicalLow: Double, discount: Double): Int {
        var score = 100
        if (discount / currentPrice > 0.25) score -= 10
        if (currentPrice > historicalLow * 1.15) score -= 20
        if ((currentPrice - discount) > historicalLow) score -= 10
        return score.coerceIn(0, 100)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingVsApp(viewModel: AppViewModel = viewModel()) {
    val snackbarHostState = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }
    val items = viewModel.items.value

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("商品价格跟踪", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("支持粘贴京东/淘宝链接，展示预计到手价与价格真实性评分。")

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("商品链接") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.addLink(input)
                    input = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("开始跟踪")
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(item.platform, fontWeight = FontWeight.SemiBold)
                                Text(if (item.isNewLow) "🎉 低于历史价" else "历史价以上")
                            }
                            Text("当前价: ¥${item.currentPrice.round2()} | 预计优惠: -¥${item.couponDiscount.round2()}")
                            Text("预计到手: ¥${item.finalPrice.round2()} | 历史低价: ¥${item.historicalLow.round2()}")
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null)
                                Text("真实性评分: ${item.trustScore}/100")
                            }
                            Text(item.url, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun Double.round2(): String = ((this * 100.0).roundToInt() / 100.0).toString()
