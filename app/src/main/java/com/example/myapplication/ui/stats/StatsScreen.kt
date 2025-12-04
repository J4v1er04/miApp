package com.example.myapplication.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun StatsScreen(
    navController: NavController,
    statsViewModel: StatsViewModel = viewModel()
) {
    val uiState by statsViewModel.uiState.collectAsState()

    // El Box con el fondo se ha eliminado. La Column ahora es el elemento raíz.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Estadísticas de Actividad", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Eventos en los Últimos 7 Días", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.last7DaysEvents.isEmpty() || uiState.last7DaysEvents.all { it.value == 0f }) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No hay datos de eventos para mostrar.")
                    }
                } else {
                    BarChart(data = uiState.last7DaysEvents)
                }
            }
        }
    }
}

@Composable
fun BarChart(data: List<BarChartData>) {
    val maxValue = data.maxOfOrNull { it.value } ?: 0f
    val density = LocalDensity.current

    // --- CORRECCIÓN ---
    // 1. Obtenemos el color en el contexto Composable
    val barColor = MaterialTheme.colorScheme.primary
    
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = with(density) { 12.sp.toPx() }
        textAlign = android.graphics.Paint.Align.CENTER
    }

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)) {
        val barWidth = size.width / (data.size * 2)
        val spaceBetweenBars = barWidth

        data.forEachIndexed { index, barData ->
            val barHeight = (barData.value / maxValue) * size.height * 0.8f
            val startX = (index * (barWidth + spaceBetweenBars)) + spaceBetweenBars / 2

            // 2. Usamos la variable con el color ya resuelto
            drawRect(
                color = barColor,
                topLeft = Offset(x = startX, y = size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )

            drawContext.canvas.nativeCanvas.drawText(
                barData.label,
                startX + barWidth / 2,
                size.height,
                textPaint
            )
            
             drawContext.canvas.nativeCanvas.drawText(
                barData.value.toInt().toString(),
                startX + barWidth / 2,
                size.height - barHeight - 5.dp.toPx(),
                textPaint
            )
        }
    }
}
