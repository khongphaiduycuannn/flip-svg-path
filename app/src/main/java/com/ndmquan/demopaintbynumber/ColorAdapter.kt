package com.ndmquan.demopaintbynumber

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ColorAdapter(
    private val colorList: List<Int>,
    private val onItemClick: (Int) -> Unit
) :
    RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    /**
     * ViewHolder chứa view cho mỗi item.
     * Trong trường hợp này, nó chỉ giữ tham chiếu đến view gốc (itemView).
     */
    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(color: Int) {
            // Set màu nền cho itemView
            itemView.setBackgroundColor(color)
            // Xử lý sự kiện khi item được click
            itemView.setOnClickListener {
                onItemClick(color)
            }
        }
    }

    /**
     * Phương thức này được gọi khi RecyclerView cần tạo một ViewHolder mới.
     * Nó sẽ inflate layout item_color.xml.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color, parent, false) // Sử dụng layout item_color.xml
        return ColorViewHolder(view)
    }

    /**
     * Phương thức này trả về tổng số item trong danh sách.
     */
    override fun getItemCount(): Int {
        return colorList.size
    }

    /**
     * Phương thức này được gọi để hiển thị dữ liệu tại một vị trí cụ thể.
     * Nó lấy màu từ danh sách và gọi hàm bind() của ViewHolder.
     */
    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        val color = colorList[position]
        holder.bind(color)
    }
}