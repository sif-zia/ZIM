import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zim.data.room.Dao.UserDao
import com.example.zim.data.room.models.UserWithCurrentUser
import com.example.zim.data.room.models.Users
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UserViewModel(private val userDao: UserDao) : ViewModel() {
    private val _currentUser = MutableStateFlow<UserWithCurrentUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    fun getCurrentUser() {
        viewModelScope.launch {
            val user = userDao.getCurrentUser()
            _currentUser.value = user
        }
    }
}
